package com.yue.jastodon.federation.following;

import java.net.URI;
import java.time.Instant;

record RemoteAccount(
        long id,
        URI actorId,
        String acct,
        URI inbox,
        String name,
        URI avatarUrl,
        Instant fetchedAt) {
}
