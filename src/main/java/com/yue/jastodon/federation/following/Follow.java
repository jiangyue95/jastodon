package com.yue.jastodon.federation.following;

import java.net.URI;
import java.time.Instant;

record Follow(long id,
              long remoteAccountId,
              URI followActivityId,
              FollowState state,
              Instant requestedAt,
              Instant acceptedAt) {
}
