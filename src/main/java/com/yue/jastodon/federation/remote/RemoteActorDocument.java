package com.yue.jastodon.federation.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
record RemoteActorDocument(URI id, URI inbox, KeyDocument publicKey, JsonNode name, JsonNode icon) {

    String displayName() {
        return null;
    }

    URI avatarUrl() {
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeyDocument(URI id, URI owner, String publicKeyPem) {
    }
}
