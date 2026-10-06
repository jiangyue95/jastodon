package com.yue.jastodon.federation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.net.URI;

@ConfigurationProperties(prefix = "jastodon.federation")
public record FederationProperties(String username, URI baseUrl, Resource privateKeyLocation) {
    public FederationProperties {
        if (username == null || !username.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "jastodon.federation.username must be lowercase letters, digits or underscores");
        }

        if (baseUrl == null || !isOrigin(baseUrl)) {
            throw new IllegalArgumentException(
                    "jastodon.federation.base-url must be an http(s) origin with no path, e.g. https://example.com");
        }

        if (privateKeyLocation == null) {
            throw new IllegalArgumentException(
                    "jastodon.federation.private-key-location must be set to the location of the private key");
        }
    }

    public URI actorId() {
        return URI.create(baseUrl + "/users/" + username);
    }

    public URI inbox() {
        return URI.create(actorId() + "/inbox");
    }

    public URI outbox() {
        return URI.create(actorId() + "/outbox");
    }

    public URI keyId() {
        return URI.create(actorId() + "#main-key");
    }

    private static boolean isOrigin(URI uri) {
        String scheme = uri.getScheme();
        return ("http".equals(scheme) || "https".equals(scheme))
                && uri.getHost() != null
                && uri.getRawPath().isEmpty()
                && uri.getRawQuery() == null
                && uri.getRawFragment() == null;
    }
}
