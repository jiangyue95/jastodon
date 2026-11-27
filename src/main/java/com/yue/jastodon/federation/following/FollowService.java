package com.yue.jastodon.federation.following;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
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
        return Optional.empty();
    }
}
