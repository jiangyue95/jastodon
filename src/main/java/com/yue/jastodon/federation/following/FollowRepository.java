package com.yue.jastodon.federation.following;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

@Repository
class FollowRepository {

    private final JdbcClient jdbcClient;

    FollowRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    long saveRequest(long remoteAccountId, URI followActivityId, Instant requestedAt) {
        return 0;
    }

    Optional<Follow> findByRemoteAccountId(long remoteAccountId) {
        return Optional.empty();
    }
}
