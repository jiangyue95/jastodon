package com.yue.jastodon.federation.inbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
record ActivityObject(URI id, String type, URI actor, URI object) {

    static ActivityObject reference(URI id) {
        return new ActivityObject(id, null, null, null);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static ActivityObject fromJsonString(String id) {
        return reference(URI.create(id));
    }
}
