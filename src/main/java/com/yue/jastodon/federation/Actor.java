package com.yue.jastodon.federation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public record Actor(
        @JsonProperty("@context") String context,
        URI id,
        String type,
        String preferredUsername,
        URI inbox,
        URI outbox) {
}
