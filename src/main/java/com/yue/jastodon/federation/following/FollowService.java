package com.yue.jastodon.federation.following;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
class FollowService {

    private final RemoteAccountRepository remoteAccountRepository;
    private final FollowRepository followRepository;
    private final DeliveryRepository deliveryRepository;
    private final FederationProperties federationProperties;
    private final JsonMapper jsonMapper;
    private final IdGenerator idGenerator;
    private final Clock clock;

    FollowService(
            RemoteAccountRepository remoteAccountRepository,
            FollowRepository followRepository,
            DeliveryRepository deliveryRepository,
            FederationProperties federationProperties,
            JsonMapper jsonMapper,
            IdGenerator idGenerator,
            Clock clock) {
        this.remoteAccountRepository = remoteAccountRepository;
        this.followRepository = followRepository;
        this.deliveryRepository = deliveryRepository;
        this.federationProperties = federationProperties;
        this.jsonMapper = jsonMapper;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    Optional<FollowResult> follow(long remoteAccountId) {
        Optional<RemoteAccount> found = remoteAccountRepository.findById(remoteAccountId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        RemoteAccount account = found.get();
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        FollowActivity activity = FollowActivity.of(
                federationProperties.actorId(),
                idGenerator.generateId(),
                account.actorId());

        Optional<Long> savedId = followRepository.saveRequest(remoteAccountId, activity.id(), now);
        if (savedId.isEmpty()) {
            Follow existing = followRepository.findByRemoteAccountId(remoteAccountId).orElseThrow();
            return Optional.of(new FollowResult(existing, false));
        }

        deliveryRepository.enqueue(account.inbox(), jsonMapper.writeValueAsString(activity), now);
        Follow created = new Follow(
                savedId.get(),
                remoteAccountId,
                activity.id(),
                FollowState.PENDING,
                now,
                null);
        return Optional.of(new FollowResult(created, true));
    }
}
