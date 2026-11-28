package com.yue.jastodon.federation.following;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
class FollowRepository {

    private final JdbcClient jdbcClient;

    FollowRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<Long> saveRequest(long remoteAccountId, URI followActivityId, Instant requestedAt) {
        return jdbcClient.sql("""
                        INSERT INTO follows (remote_account_id, follow_activity_id, state, requested_at)
                        VALUES (:remoteAccountId, :followActivityId, :state, :requestedAt)
                        ON CONFLICT (remote_account_id) DO NOTHING
                        RETURNING id
                        """)
                .param("remoteAccountId", remoteAccountId)
                .param("followActivityId", followActivityId.toString())
                .param("state", FollowState.PENDING.name())
                .param("requestedAt", requestedAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .optional();
    }

    Optional<Follow> findByRemoteAccountId(long remoteAccountId) {
        return jdbcClient.sql("""
                              SELECT id, remote_account_id, follow_activity_id, state, requested_at, accepted_at
                              FROM follows
                              where remote_account_id = :remoteAccountId
                              """)
                .param("remoteAccountId", remoteAccountId)
                .query(FollowRepository::mapRow)
                .optional();
    }

    boolean markAccepted(URI actorId, URI followActivityId, Instant acceptedAt) {
        return jdbcClient.sql("""
                       UPDATE follows
                       SET state = :state, accepted_at = COALESCE(accepted_at, :acceptedAt)
                       WHERE follow_activity_id = :followActivityId
                         AND remote_account_id = (SELECT id FROM remote_accounts WHERE actor_id = :actorId)
                       """)
                .param("state", FollowState.ACCEPTED.name())
                .param("acceptedAt", acceptedAt.atOffset(ZoneOffset.UTC))
                .param("followActivityId", followActivityId.toString())
                .param("actorId", actorId.toString())
                .update() > 0;
    }

    boolean remove(URI actorId, URI followActivityId) {
        return jdbcClient.sql("""
                       DELETE FROM follows
                       WHERE follow_activity_id = :followActivityId
                         AND remote_account_id = (SELECT id FROM remote_accounts WHERE actor_id = :actorId)
                       """)
                .param("followActivityId", followActivityId.toString())
                .param("actorId", actorId.toString())
                .update() > 0;
    }

    private static Follow mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime acceptedAt = rs.getObject("accepted_at", OffsetDateTime.class);
        return new Follow(
                rs.getLong("id"),
                rs.getLong("remote_account_id"),
                URI.create(rs.getString("follow_activity_id")),
                FollowState.valueOf(rs.getString("state")),
                rs.getObject("requested_at", OffsetDateTime.class).toInstant(),
                acceptedAt == null ? null : acceptedAt.toInstant());
    }
}
