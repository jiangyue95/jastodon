package com.yue.jastodon.federation.following;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;

@Component
public class FollowReplies {

    private final FollowRepository followRepository;
    private final Clock clock;

    FollowReplies(FollowRepository followRepository, Clock clock) {
        this.followRepository = followRepository;
        this.clock = clock;
    }

    public boolean accept(URI actorId, URI followActivityId) {
        return false;
    }

    public boolean reject(URI actorId, URI followActivityId) {
        return false;
    }
}
