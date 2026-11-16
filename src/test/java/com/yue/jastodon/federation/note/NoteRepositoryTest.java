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
    private static final UUID THIRD_ID = UUID.fromString("2e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");
    private static final UUID FOURTH_ID = UUID.fromString("3e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");
    private static final String CONTENT = "hello";
    private static final Instant EARLIEST = Instant.parse("2026-10-07T09:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-07T11:00:00Z");
    private static final Instant LATEST = Instant.parse("2026-10-07T12:00:00Z");

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

    @Test
    void findsNoteById() {
        Note wanted = new Note(ID, CONTENT, EARLIER);
        Note other = new Note(OTHER_ID, CONTENT, LATER);
        noteRepository.save(other);
        noteRepository.save(wanted);

        assertThat(noteRepository.findById(ID)).contains(wanted);
    }

    @Test
    void findsNothingForAnUnknownId() {
        noteRepository.save(new Note(ID, CONTENT, EARLIER));

        assertThat(noteRepository.findById(OTHER_ID)).isEmpty();
    }

    @Test
    void firstPageHoldsTheNewestNotesUpToTheLimit() {
        Note newest = new Note(ID, CONTENT, LATEST);
        Note middle = new Note(THIRD_ID, CONTENT, LATER);
        Note oldest = new Note(OTHER_ID, CONTENT, EARLIER);
        noteRepository.save(middle);
        noteRepository.save(newest);
        noteRepository.save(oldest);

        assertThat(noteRepository.findFirstPage(2)).containsExactly(newest, middle);
    }

    @Test
    void firstPageOrdersNotesOfTheSameInstantByIdDescending() {
        Note smallerId = new Note(ID, CONTENT, EARLIER);
        Note largerId = new Note(OTHER_ID, CONTENT, EARLIER);
        noteRepository.save(smallerId);
        noteRepository.save(largerId);

        assertThat(noteRepository.findFirstPage(2)).containsExactly(largerId, smallerId);
    }

    @Test
    void pageAfterACursorHoldsTheNextNotesUpToTheLimit() {
        Note newest = new Note(ID, CONTENT, LATEST);
        Note second = new Note(FOURTH_ID, CONTENT, LATER);
        Note third = new Note(OTHER_ID, CONTENT, EARLIER);
        Note oldest = new Note(THIRD_ID, CONTENT, EARLIEST);
        noteRepository.save(third);
        noteRepository.save(oldest);
        noteRepository.save(newest);
        noteRepository.save(second);

        NoteCursor cursor = new NoteCursor(newest.published(), newest.id());

        assertThat(noteRepository.findPageAfter(cursor, 2)).containsExactly(second, third);
    }

    @Test
    void pageAfterACursorSplitsNotesOfTheSameInstantById() {
        Note largest = new Note(THIRD_ID, CONTENT, EARLIER);
        Note between = new Note(OTHER_ID, CONTENT, EARLIER);
        Note smallest = new Note(ID, CONTENT, EARLIER);
        noteRepository.save(smallest);
        noteRepository.save(largest);
        noteRepository.save(between);

        NoteCursor cursor = new NoteCursor(between.published(), between.id());

        assertThat(noteRepository.findPageAfter(cursor, 2)).containsExactly(smallest);
    }

    @Test
    void pageAfterACursorDoesNotNeedItsNoteToExist() {
        Note newer = new Note(ID, CONTENT, LATEST);
        Note older = new Note(OTHER_ID, CONTENT, EARLIER);
        noteRepository.save(newer);
        noteRepository.save(older);

        NoteCursor cursorOfADeletedNote = new NoteCursor(LATER, THIRD_ID);

        assertThat(noteRepository.findPageAfter(cursorOfADeletedNote, 2)).containsExactly(older);
    }
}
