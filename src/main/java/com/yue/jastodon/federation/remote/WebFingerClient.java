package com.yue.jastodon.federation.remote;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Component
public class WebFingerClient {

    private static final MediaType JRD_JSON = MediaType.parseMediaType("application/jrd+json");
    private static final Set<String> ACTIVITYPUB_TYPES = Set.of(
            "application/activity+json",
            "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"");

    private final ActivityPubClient activityPubClient;

    WebFingerClient(ActivityPubClient activityPubClient) {
        this.activityPubClient = activityPubClient;
    }

    public WebFingerAccount resolve(Acct acct) {
        String resource = "acct:" + acct.value();
        URI url = URI.create("https://" + acct.host() + "/.well-known/webfinger?resource=" + resource);
        WebFingerDocument document = activityPubClient.getUnsigned(
                url,
                WebFingerDocument.class,
                JRD_JSON,
                MediaType.APPLICATION_JSON);
        if (document == null) {
            throw new RemoteFetchException("WebFinger returned an empty response for " + resource);
        }
        if (!resource.equalsIgnoreCase(document.subject())) {
            throw new RemoteFetchException("WebFinger subject " + document.subject() + " does not match " + resource);
        }
        Acct confirmed = Acct.parse(document.subject().substring("acct:".length())).orElseThrow();
        List<WebFingerDocument.Link> links = document.links() == null ? List.of() : document.links();
        for (WebFingerDocument.Link link : links) {
            if (link != null
                    && "self".equals(link.rel())
                    && link.type() != null
                    && ACTIVITYPUB_TYPES.contains(link.type())) {
                if (link.href() == null) {
                    throw new RemoteFetchException("WebFinger self link for " + resource + " has no href");
                }
                return new WebFingerAccount(confirmed, link.href());
            }
        }
        throw new RemoteFetchException("WebFinger response for " + resource + " has no ActivityPub self link");
    }
}
