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
class RemoteAccountRepository {

    private final JdbcClient jdbcClient;

    RemoteAccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    long save(URI actorId, String acct, URI inbox, String name, URI avatarUrl, Instant fetchedAt) {
        return jdbcClient.sql("""
                              INSERT INTO remote_accounts (actor_id, acct, inbox, name, avatar_url, fetched_at)
                              VALUES (:actorId, :acct, :inbox, :name, :avatarUrl, :fetchedAt)
                              ON CONFLICT (actor_id) DO UPDATE
                              SET acct = EXCLUDED.acct,
                                  inbox = EXCLUDED.inbox,
                                  name = EXCLUDED.name,
                                  avatar_url = EXCLUDED.avatar_url,
                                  fetched_at = EXCLUDED.fetched_at
                              RETURNING id
                              """)
                .param("actorId", actorId.toString())
                .param("acct", acct)
                .param("inbox", inbox.toString())
                .param("name", name)
                .param("avatarUrl", avatarUrl == null ? null : avatarUrl.toString())
                .param("fetchedAt", fetchedAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    Optional<RemoteAccount> findByActorId(URI actorId) {
        return jdbcClient.sql("""
                              SELECT id, actor_id, acct, inbox, name, avatar_url, fetched_at
                              FROM remote_accounts
                              WHERE actor_id = :actorId
                              """)
                .param("actorId", actorId.toString())
                .query(RemoteAccountRepository::mapRow)
                .optional();
    }

    private static RemoteAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
        String avatarUrl = rs.getString("avatar_url");
        return new RemoteAccount(
                rs.getLong("id"),
                URI.create(rs.getString("actor_id")),
                rs.getString("acct"),
                URI.create(rs.getString("inbox")),
                rs.getString("name"),
                avatarUrl == null ? null : URI.create(avatarUrl),
                rs.getObject("fetched_at", OffsetDateTime.class).toInstant());
    }
}
