package com.yue.jastodon.federation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

public record Actor(
        @JsonProperty("@context") List<String> context,
        URI id,
        String type,
        String preferredUsername,
        URI inbox,
        URI outbox,
        PublicKey publicKey) {

    public record PublicKey(URI id, URI owner, String publicKeyPem) {}
}
