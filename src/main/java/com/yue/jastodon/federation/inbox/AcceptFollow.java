package com.yue.jastodon.federation.inbox;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

record AcceptFollow(
        @JsonProperty("@context") String context,
        URI id,
        String type,
        URI actor,
        Follow object) {

    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";

    record Follow(URI id, String type, URI actor, URI object) {
    }

    static AcceptFollow of(URI actorId, URI followId, URI followerId) {
        return new AcceptFollow(
                ACTIVITY_STREAMS_CONTEXT,
                URI.create(actorId + "#accepts/follows/" + sha256Hex(followId.toString())),
                "Accept",
                actorId,
                new Follow(followId, "Follow", followerId, actorId));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
