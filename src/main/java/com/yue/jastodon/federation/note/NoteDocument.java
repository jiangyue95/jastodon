package com.yue.jastodon.federation.note;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yue.jastodon.federation.FederationProperties;

import java.net.URI;
import java.time.Instant;
import java.util.List;

record NoteDocument(
        @JsonProperty("@context") String context,
        URI id,
        String type,
        URI attributedTo,
        String content,
        Instant published,
        List<URI> to) {

    static NoteDocument of(Note note, FederationProperties federation) {
        return null;
    }
}
