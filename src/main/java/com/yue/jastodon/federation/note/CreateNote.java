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

    static CreateNote of(NoteDocument note) {
        return new CreateNote(
                note.context(),
                null,
                "Create",
                null,
                null,
                null,
                null);
    }
}
