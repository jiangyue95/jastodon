package com.yue.jastodon.federation;

import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class ActivityHandler {

    private final FederationProperties federationProperties;
    private final RemoteActorFetcher remoteActorFetcher;
    private final FollowerRepository followerRepository;
    private final Clock clock;

    public ActivityHandler(
            FederationProperties federationProperties,
            RemoteActorFetcher remoteActorFetcher,
            FollowerRepository followerRepository,
            Clock clock) {
        this.federationProperties = federationProperties;
        this.remoteActorFetcher = remoteActorFetcher;
        this.followerRepository = followerRepository;
        this.clock = clock;
    }

    public void handle(IncomingActivity activity) {
        if ("Follow".equals(activity.type())) {
            handleFollow(activity);
        }
    }

    private void handleFollow(IncomingActivity follow) {
        if (!federationProperties.actorId().equals(follow.object())) {
            return;
        }
        RemoteActor actor = remoteActorFetcher.fetch(follow.actor());
        followerRepository.save(new Follower(follow.actor(), actor.inbox(), follow.id(), clock.instant()));
    }
}
