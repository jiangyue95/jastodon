package com.yue.jastodon.federation.note;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yue.jastodon.federation.FederationProperties;

import java.net.URI;
import java.util.List;

record Outbox(
        @JsonProperty("@context") String context,
        URI id,
        String type,
        int totalItems,
        List<CreateNote> orderedItems) {

    private static final String ACTIVITY_STREAMS_CONTEXT = "https://www.w3.org/ns/activitystreams";

    static Outbox of(List<Note> notes, FederationProperties federation) {
        List<CreateNote> items = notes.stream()
                .map(note -> CreateNote.of(NoteDocument.of(note, federation)))
                .toList();

        return new Outbox(
                ACTIVITY_STREAMS_CONTEXT,
                federation.outbox(),
                "OrderedCollection",
                items.size(),
                items);
    }
}
