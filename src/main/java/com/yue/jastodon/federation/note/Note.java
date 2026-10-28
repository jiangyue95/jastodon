package com.yue.jastodon.federation.note;

import java.time.Instant;
import java.util.UUID;

record Note(UUID id, String content, Instant published) {
    Note {
        if (id == null) {
            throw new IllegalArgumentException("Note id must not be null");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Note content must not be blank");
        }

        if (published == null) {
            throw new IllegalArgumentException("Note published must not be null");
        }
    }
}
