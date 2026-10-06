package com.yue.jastodon.federation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "jastodon.federation")
public record FederationProperties(String username, URI baseUrl) {
    public FederationProperties {
        if (username == null || !username.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "jastodon.federation.username must be lowercase letters, digits or underscores");
        }

        if (baseUrl == null || !isOrigin(baseUrl)) {
            throw new IllegalArgumentException(
                    "jastodon.federation.base-url must be an http(s) origin with no path, e.g. https://example.com");
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

    private static boolean isOrigin(URI uri) {
        String scheme = uri.getScheme();
        return ("http".equals(scheme) || "https".equals(scheme))
                && uri.getHost() != null
                && uri.getRawPath().isEmpty()
                && uri.getRawQuery() == null
                && uri.getRawFragment() == null;
    }
}
