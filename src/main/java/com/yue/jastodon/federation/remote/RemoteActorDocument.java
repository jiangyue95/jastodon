package com.yue.jastodon.federation.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
record RemoteActorDocument(URI id, URI inbox, KeyDocument publicKey, JsonNode name, JsonNode icon) {

    String displayName() {
        return name != null && name.isString() ? name.stringValue() : null;
    }

    URI avatarUrl() {
        JsonNode image = icon != null && icon.isArray() ? icon.get(0) : icon;
        if (image == null || !image.isObject()) {
            return null;
        }
        JsonNode url = image.get("url");
        if (url != null && url.isObject()) {
            url = url.get("href");
        }
        if (url == null || !url.isString()) {
            return null;
        }
        try {
            URI uri = URI.create(url.stringValue());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null ? uri : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeyDocument(URI id, URI owner, String publicKeyPem) {
    }
}
