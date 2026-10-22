package com.yue.jastodon.federation;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.PublicKey;

@Component
public class RemoteActorKeyFetcher {

    private final ActivityPubClient activityPubClient;

    public RemoteActorKeyFetcher(ActivityPubClient activityPubClient) {
        this.activityPubClient = activityPubClient;
    }

    public RemoteKey fetch(URI keyId) {
        URI documentUrl = withoutFragment(keyId);
        RemoteActorDocument document = activityPubClient.get(documentUrl, RemoteActorDocument.class);

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
