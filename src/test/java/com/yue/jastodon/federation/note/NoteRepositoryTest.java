package com.yue.jastodon.federation.note;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NoteRepositoryTest extends AbstractIntegrationTest {

    private static final UUID ID = UUID.fromString("0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");
    private static final UUID OTHER_ID = UUID.fromString("1e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");
    private static final String CONTENT = "hello";
    private static final Instant EARLIER = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-07T11:00:00Z");


    @Autowired
    NoteRepository noteRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void emptyTable() {
        jdbcClient.sql("DELETE FROM notes").update();
    }

    @Test
    void savesAndListNote() {
        Note note = new Note(ID, CONTENT, EARLIER);

        noteRepository.save(note);

        assertThat(noteRepository.findAll()).containsExactly(note);
    }

    @Test
    void listsNewestFirst() {
        Note older = new Note(ID, CONTENT, EARLIER);
        Note newer = new Note(OTHER_ID, CONTENT, LATER);

        noteRepository.save(older);
        noteRepository.save(newer);

        assertThat(noteRepository.findAll()).containsExactly(newer, older);
    }

    @Test
    void rejectsDuplicatedId() {
        Note first = new Note(ID, CONTENT, EARLIER);
        Note duplicate = new Note(ID, CONTENT, LATER);
        noteRepository.save(first);

        assertThatThrownBy(() -> noteRepository.save(duplicate))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("notes_pkey");
    }
}
