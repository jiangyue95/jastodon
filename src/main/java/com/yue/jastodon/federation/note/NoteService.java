package com.yue.jastodon.federation.note;

import org.springframework.stereotype.Service;
import org.springframework.util.IdGenerator;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Service
class NoteService {

    private final NoteRepository noteRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    NoteService(NoteRepository noteRepository, IdGenerator idGenerator, Clock clock) {
        this.noteRepository = noteRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    Note create(String content) {
        Note note = new Note(idGenerator.generateId(), content, clock.instant().truncatedTo(ChronoUnit.MICROS));
        noteRepository.save(note);
        return note;
    }
}
