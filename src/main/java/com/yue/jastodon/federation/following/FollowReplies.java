package com.yue.jastodon.federation.following;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class FollowReplies {

    private final FollowRepository followRepository;
    private final Clock clock;

    FollowReplies(FollowRepository followRepository, Clock clock) {
        this.followRepository = followRepository;
        this.clock = clock;
    }

    public boolean accept(URI actorId, URI followActivityId) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        return followRepository.markAccepted(actorId, followActivityId, now);
    }

    public boolean reject(URI actorId, URI followActivityId) {
        return followRepository.remove(actorId, followActivityId);
    }
}
