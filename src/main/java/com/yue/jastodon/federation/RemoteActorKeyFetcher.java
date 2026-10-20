package com.yue.jastodon.federation;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.security.PublicKey;

@Component
public class RemoteActorKeyFetcher {

    private static final MediaType ACTIVITY_JSON = MediaType.parseMediaType("application/activity+json");
    private static final MediaType LD_JSON_ACTIVITYSTREAMS =
            MediaType.parseMediaType("application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"");

    private final RestClient restClient;
    private final OutboundUrlPolicy outboundUrlPolicy;

    public RemoteActorKeyFetcher(RestClient restClient, OutboundUrlPolicy outboundUrlPolicy) {
        this.restClient = restClient;
        this.outboundUrlPolicy = outboundUrlPolicy;
    }

    public RemoteKey fetch(URI keyId) {
        URI documentUrl = withoutFragment(keyId);

        try {
            outboundUrlPolicy.check(documentUrl);
        } catch (UnsafeRemoteUrlException e) {
            throw new RemoteFetchException("Refusing to fetch " + documentUrl + ": " + e.getMessage());
        }

        RemoteActorDocument document;
        try {
            document = restClient.get()
                    .uri(documentUrl)
                    .accept(ACTIVITY_JSON, LD_JSON_ACTIVITYSTREAMS)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (request, response) -> {
                        throw new RemoteFetchException(
                                "Unexpected status " + response.getStatusCode() + " from " + documentUrl);
                    })
                    .body(RemoteActorDocument.class);
        } catch (RestClientException e) {
            throw new RemoteFetchException("Cannot fetch " + documentUrl, e);
        }

        if (document == null || document.publicKey() == null) {
            throw new RemoteFetchException("No publicKey in " + documentUrl);
        }
        RemoteActorDocument.KeyDocument key = document.publicKey();

        if (!keyId.equals(key.id())) {
            throw new RemoteFetchException("publicKey.id does not match " + keyId);
        }
        if (key.owner() == null || !hostOf(keyId).equalsIgnoreCase(hostOf(key.owner()))) {
            throw new RemoteFetchException("publicKey.owner is not on the same host as " + keyId);
        }
        if (key.publicKeyPem() == null) {
            throw new RemoteFetchException("No publicKeyPem in " + documentUrl);
        }

        PublicKey publicKey;
        try {
            publicKey = PublicKeyPem.parse(key.publicKeyPem());
        } catch (IllegalArgumentException e) {
            throw new RemoteFetchException("Invalid public key in " + documentUrl, e);
        }

        return new RemoteKey(keyId, key.owner(), publicKey);
    }

    private static URI withoutFragment(URI uri) {
        String value = uri.toString();
        int hash = value.indexOf("#");
        return hash < 0 ? uri : URI.create(value.substring(0, hash));
    }

    private static String hostOf(URI uri) {
        return uri.getHost() == null ? "" : uri.getHost();
    }
}
