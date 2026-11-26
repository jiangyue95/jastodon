package com.yue.jastodon.federation.following;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.UUID;

record FollowActivity(
        @JsonProperty("@context") String context,
        URI id,
        String type,
        URI actor,
        URI object) {

    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";

    static FollowActivity of(URI actorId, UUID id, URI targetActorId) {
        return new FollowActivity(
                ACTIVITY_STREAMS_CONTEXT,
                URI.create(actorId + "#follows/" + id),
                "Follow",
                actorId,
                targetActorId);
    }
}
