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

    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";
    private static final String SECURITY_CONTEXT = "https://w3id.org/security/v1";
    private static final URI PUBLIC = URI.create("https://www.w3.org/ns/activitystreams#Public");

    static UpdatePerson of(Actor person, UUID updateId) {
        return new UpdatePerson(
                List.of(ACTIVITY_STREAMS_CONTEXT, SECURITY_CONTEXT),
                URI.create(person.id() + "#updates/" + updateId),
                "Update",
                person.id(),
                List.of(PUBLIC),
                person.withoutContext());
    }
}
