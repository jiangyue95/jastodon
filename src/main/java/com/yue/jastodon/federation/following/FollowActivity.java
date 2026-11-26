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

    static FollowActivity of(URI actorId, UUID id, URI targetActorId) {
        return new FollowActivity(null, null, null, null, null);
    }
}
