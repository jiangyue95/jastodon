package com.yue.jastodon.federation.actor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;
import java.util.UUID;

record UpdatePerson(
        @JsonProperty("@context") List<String> context,
        URI id,
        String type,
        URI actor,
        List<URI> to,
        Actor object) {

    static UpdatePerson of(Actor person, UUID updateId) {
        return new UpdatePerson(null, null, null, null, null, null);
    }
}
