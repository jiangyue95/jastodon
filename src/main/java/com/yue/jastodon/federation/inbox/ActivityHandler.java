package com.yue.jastodon.federation.inbox;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import com.yue.jastodon.federation.follower.Follower;
import com.yue.jastodon.federation.follower.FollowerRepository;
import com.yue.jastodon.federation.remote.RemoteActor;
import com.yue.jastodon.federation.remote.RemoteActorFetcher;
import com.yue.jastodon.federation.remote.RemoteFetchException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

@Component
public class ActivityHandler {

    private final FederationProperties federationProperties;
    private final RemoteActorFetcher remoteActorFetcher;
    private final FollowerRepository followerRepository;
    private final DeliveryRepository deliveryRepository;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public ActivityHandler(
            FederationProperties federationProperties,
            RemoteActorFetcher remoteActorFetcher,
            FollowerRepository followerRepository,
            DeliveryRepository deliveryRepository,
            JsonMapper jsonMapper,
            Clock clock,
            TransactionTemplate transactionTemplate) {
        this.federationProperties = federationProperties;
        this.remoteActorFetcher = remoteActorFetcher;
        this.followerRepository = followerRepository;
        this.deliveryRepository = deliveryRepository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    public void handle(IncomingActivity activity) {
        if ("Follow".equals(activity.type())) {
            handleFollow(activity);
        }
    }

    private void handleFollow(IncomingActivity follow) {
        if (follow.id() == null) {
            throw new InvalidActivityException("Follow has no id");
        }
        if (!federationProperties.actorId().equals(follow.object())) {
            return;
        }
        RemoteActor actor = fetchActor(follow.actor());
        Instant now = clock.instant();
        AcceptFollow accept = AcceptFollow.of(federationProperties.actorId(), follow.id(), follow.actor());
        String body = jsonMapper.writeValueAsString(accept);
        transactionTemplate.executeWithoutResult(status -> {
            followerRepository.save(new Follower(follow.actor(), actor.inbox(), follow.id(), now));
            deliveryRepository.enqueue(actor.inbox(), body, now);
        });
    }

    private RemoteActor fetchActor(URI actorId) {
        try {
            return remoteActorFetcher.fetch(actorId);
        } catch (RemoteFetchException e) {
            throw new RetryLaterException("Cannot fetch actor " + actorId, e);
        }
    }
}
