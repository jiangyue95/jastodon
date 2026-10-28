package com.yue.jastodon.federation.note;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NoteServiceTest extends AbstractIntegrationTest {

    private static final UUID ID = UUID.fromString("0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-07T10:00:00.123456789Z"), ZoneOffset.UTC);

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    JdbcClient jdbcClient;

    NoteService noteService;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM notes").update();
        noteService = new NoteService(noteRepository, () -> ID, CLOCK);
    }

    @Test
    void createReturnsTheStoredNote() {
        Note created = noteService.create("hello");

        assertThat(created).isEqualTo(new Note(
                ID,
                "hello",
                Instant.parse("2026-10-07T10:00:00.123456Z")));
        assertThat(noteRepository.findAll()).containsExactly(created);
    }
}
