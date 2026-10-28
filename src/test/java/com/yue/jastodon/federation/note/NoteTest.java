package com.yue.jastodon.federation.note;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoteTest {

    private static final UUID ID = UUID.fromString("0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");
    private static final String CONTENT = "hello";
    private static final Instant PUBLISHED = Instant.parse("2026-10-07T10:00:00Z");

    @Test
    void createsNoteWithAllFields() {
        Note note = new Note(ID, CONTENT, PUBLISHED);

        assertThat(note.id()).isEqualTo(ID);
        assertThat(note.content()).isEqualTo(CONTENT);
        assertThat(note.published()).isEqualTo(PUBLISHED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n"})
    void rejectsBlankContent(String content) {
        assertThatThrownBy(() -> new Note(ID, content, PUBLISHED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Note content must not be blank");
    }

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> new Note(null, CONTENT, PUBLISHED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Note id must not be null");
    }

    @Test
    void rejectsNullPublished() {
        assertThatThrownBy(() -> new Note(ID, CONTENT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Note published must not be null");
    }
}
