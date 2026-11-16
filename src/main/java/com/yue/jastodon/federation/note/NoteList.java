package com.yue.jastodon.federation.note;

import com.yue.jastodon.federation.FederationProperties;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record NoteList(List<Item> notes) {

    record Item(
            UUID id,
            String content,
            Instant published,
            URI url) {
    }

    static NoteList of(List<Note> notes, FederationProperties federation) {
        List<Item> items = notes.stream()
                .map(note -> new Item(
                        note.id(),
                        note.content(),
                        note.published(),
                        federation.noteId(note.id())))
                .toList();

        return new NoteList(items);
    }
}
