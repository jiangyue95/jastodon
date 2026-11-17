package com.yue.jastodon.federation.note;

import com.yue.jastodon.federation.FederationProperties;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record NoteList(List<Item> notes, String next) {

    record Item(
            UUID id,
            String content,
            Instant published,
            URI url) {
    }

    static NoteList of(NotePage page, FederationProperties federation) {
        List<Item> items = page.notes().stream()
                .map(note -> new Item(
                        note.id(),
                        note.content(),
                        note.published(),
                        federation.noteId(note.id())))
                .toList();

        String next = page.next().map(NoteCursor::encode).orElse(null);
        return new NoteList(items, next);
    }
}
