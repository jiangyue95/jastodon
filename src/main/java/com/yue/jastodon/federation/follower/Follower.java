package com.yue.jastodon.federation.follower;

import java.net.URI;
import java.time.Instant;

public record Follower(URI actorId, URI inbox, URI followActivityId, Instant followedAt) {
}
