package com.yue.jastodon.federation.note;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import com.yue.jastodon.federation.follower.FollowerRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.IdGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Service
class NoteService {

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

    Note create(String content) {
        Note note = new Note(idGenerator.generateId(), content, clock.instant().truncatedTo(ChronoUnit.MICROS));
        noteRepository.save(note);
        return note;
    }
}
