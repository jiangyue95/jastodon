package com.yue.jastodon.federation.actor;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import com.yue.jastodon.federation.follower.Follower;
import com.yue.jastodon.federation.follower.FollowerRepository;
import com.yue.jastodon.federation.profile.Avatars;
import org.springframework.stereotype.Service;
import org.springframework.util.IdGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

@Service
class AvatarService {

    private final Avatars avatars;
    private final FederationProperties federationProperties;
    private final ActorDocumentFactory actorDocumentFactory;
    private final FollowerRepository followerRepository;
    private final DeliveryRepository deliveryRepository;
    private final JsonMapper jsonMapper;
    private final IdGenerator idGenerator;
    private final Clock clock;

    AvatarService(
            Avatars avatars,
            FederationProperties federationProperties,
            ActorDocumentFactory actorDocumentFactory,
            FollowerRepository followerRepository,
            DeliveryRepository deliveryRepository,
            JsonMapper jsonMapper,
            IdGenerator idGenerator,
            Clock clock) {
        this.avatars = avatars;
        this.federationProperties = federationProperties;
        this.actorDocumentFactory = actorDocumentFactory;
        this.followerRepository = followerRepository;
        this.deliveryRepository = deliveryRepository;
        this.jsonMapper = jsonMapper;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    URI change(byte[] image) {
        String key = avatars.upload(image);
        String username = federationProperties.username();
        avatars.makeCurrent(username, key);
        Actor actor = actorDocumentFactory.build();
        String body = jsonMapper.writeValueAsString(UpdatePerson.of(actor, idGenerator.generateId()));
        Instant now = clock.instant();
        for (Follower follower : followerRepository.findAll()) {
            deliveryRepository.enqueue(follower.inbox(), body, now);
        }
        return avatars.currentUrl(username).orElseThrow();
    }
}
