package com.yue.jastodon.federation.note;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.time.Instant;
import java.util.List;

record CreateNote(
        @JsonProperty("@context") String context,
        URI id,
        String type,
        URI actor,
        Instant published,
        List<URI> to,
        NoteDocument object) {

    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";

    static CreateNote of(NoteDocument note) {
        return new CreateNote(
                ACTIVITY_STREAMS_CONTEXT,
                URI.create(note.id() + "/activity"),
                "Create",
                note.attributedTo(),
                note.published(),
                note.to(),
                note.withoutContext());
    }
}
