package com.yue.jastodon.federation.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("jastodon.media")
record MediaProperties(URI baseUrl) {

    MediaProperties {
        if (baseUrl == null || !isHttpsOrigin(baseUrl)) {
            throw new IllegalArgumentException(
                    "jastodon.media.base-url must be an https origin with no path, e.g. https://media.example.com");
        }
    }

    private static boolean isHttpsOrigin(URI uri) {
        return "https".equals(uri.getScheme())
                && uri.getHost() != null
                && uri.getRawUserInfo() == null
                && uri.getRawPath().isEmpty()
                && uri.getRawQuery() == null
                && uri.getRawFragment() == null;
    }
}
