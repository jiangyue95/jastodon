package com.yue.jastodon.federation.actor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

public record Actor(
        @JsonProperty("@context") @JsonInclude(JsonInclude.Include.NON_NULL) List<String> context,
        URI id,
        String type,
        String preferredUsername,
        URI inbox,
        URI outbox,
        PublicKey publicKey,
        @JsonInclude(JsonInclude.Include.NON_NULL) Icon icon) {

    Actor withoutContext() {
        return new Actor(
                null,
                id,
                type,
                preferredUsername,
                inbox,
                outbox,
                publicKey,
                icon);
    }

    public record PublicKey(URI id, URI owner, String publicKeyPem) {}

    public record Icon(String type, String mediaType, URI url) {}
}
