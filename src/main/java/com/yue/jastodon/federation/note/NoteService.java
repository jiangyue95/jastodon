package com.yue.jastodon.federation.note;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import com.yue.jastodon.federation.follower.Follower;
import com.yue.jastodon.federation.follower.FollowerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
class NoteService {

    private static final int PAGE_SIZE = 20;

    private final NoteRepository noteRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final FollowerRepository followerRepository;
    private final DeliveryRepository deliveryRepository;
    private final FederationProperties federationProperties;
    private final JsonMapper jsonMapper;

    NoteService(
            NoteRepository noteRepository,
            IdGenerator idGenerator,
            Clock clock,
            FollowerRepository followerRepository,
            DeliveryRepository deliveryRepository,
            FederationProperties federationProperties,
            JsonMapper jsonMapper) {
        this.noteRepository = noteRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.followerRepository = followerRepository;
        this.deliveryRepository = deliveryRepository;
        this.federationProperties = federationProperties;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    Note create(String content) {
        Note note = new Note(idGenerator.generateId(), content, clock.instant().truncatedTo(ChronoUnit.MICROS));
        noteRepository.save(note);

        String body = jsonMapper.writeValueAsString(CreateNote.of(NoteDocument.of(note, federationProperties)));
        for (Follower follower : followerRepository.findAll()) {
            deliveryRepository.enqueue(follower.inbox(), body, note.published());
        }
        return note;
    }

    NotePage firstPage() {
        return toPage(noteRepository.findFirstPage(PAGE_SIZE + 1));
    }

    NotePage pageAfter(NoteCursor cursor) {
        return toPage(noteRepository.findPageAfter(cursor, PAGE_SIZE + 1));
    }

    private static NotePage toPage(List<Note> fetched) {
        if (fetched.size() <= PAGE_SIZE) {
            return new NotePage(fetched, Optional.empty());
        }
        List<Note> notes = fetched.subList(0, PAGE_SIZE);
        Note last = notes.getLast();
        NoteCursor cursor = new NoteCursor(last.published(), last.id());
        return new NotePage(notes, Optional.of(cursor));
    }
}
