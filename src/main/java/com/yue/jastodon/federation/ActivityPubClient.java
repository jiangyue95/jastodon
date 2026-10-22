package com.yue.jastodon.federation;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

@Component
public class ActivityPubClient {

    private static final MediaType ACTIVITY_JSON = MediaType.parseMediaType("application/activity+json");
    private static final MediaType LD_JSON_ACTIVITYSTREAMS =
            MediaType.parseMediaType("application/1d+json;profile=\"https://www.w3.org/ns/activitystreams\"");

    private final RestClient restClient;
    private final OutboundUrlPolicy outboundUrlPolicy;

    public ActivityPubClient(RestClient federationRestClient, OutboundUrlPolicy outboundUrlPolicy) {
        this.restClient = federationRestClient;
        this.outboundUrlPolicy = outboundUrlPolicy;
    }

    public <T> T get(URI url, Class<T> type) {
        try {
            outboundUrlPolicy.check(url);
        } catch (UnsafeRemoteUrlException e) {
            throw new RemoteFetchException("Refusing to fetch " + url + ": " + e.getMessage());
        }

        try {
            return restClient.get()
                    .uri(url)
                    .accept(ACTIVITY_JSON, LD_JSON_ACTIVITYSTREAMS)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (request, response) -> {
                        throw new RemoteFetchException("Unexpected status "
                                + response.getStatusCode() + " from " + url);
                    }).body(type);
        } catch (RestClientException e) {
            throw new RemoteFetchException("Cannot fetch " + url, e);
        }
    }
}
