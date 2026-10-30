package com.yue.jastodon.federation.note;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yue.jastodon.federation.FederationProperties;

import java.net.URI;
import java.time.Instant;
import java.util.List;

record NoteDocument(
        @JsonProperty("@context") @JsonInclude(JsonInclude.Include.NON_NULL) String context,
        URI id,
        String type,
        URI attributedTo,
        String content,
        Instant published,
        List<URI> to) {

    private static final URI PUBLIC = URI.create("https://www.w3.org/ns/activitystreams#Public");
    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";

    NoteDocument withoutContext() {
        return new NoteDocument(
                null,
                id,
                type,
                attributedTo,
                content,
                published,
                to);
    }

    static NoteDocument of(Note note, FederationProperties federation) {
        return new NoteDocument(
                ACTIVITY_STREAMS_CONTEXT,
                federation.noteId(note.id()),
                "Note",
                federation.actorId(),
                NoteContentRenderer.render(note.content()),
                note.published(),
                List.of(PUBLIC));
    }
}
