package com.yue.jastodon.federation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class FollowerRepository {

    private final JdbcClient jdbcClient;

    public FollowerRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(Follower follower) {
        jdbcClient.sql("""
                       INSERT INTO followers (actor_id, inbox, follow_activity_id, followed_at)
                       VALUES (:actorId, :inbox, :followActivityId, :followedAt)
                       ON CONFLICT (actor_id) DO UPDATE 
                       SET inbox = EXCLUDED.inbox,
                           follow_activity_id = EXCLUDED.follow_activity_id
                       """)
                .param("actorId", follower.actorId().toString())
                .param("inbox", follower.inbox().toString())
                .param("followActivityId", follower.followActivityId().toString())
                .param("followedAt", follower.followedAt().atOffset(ZoneOffset.UTC))
                .update();
    }

    public List<Follower> findAll() {
        return jdbcClient.sql("""
                              SELECT actor_id, inbox, follow_activity_id, followed_at
                              FROM followers
                              ORDER BY followed_at, id
                              """)
                .query(FollowerRepository::mapRow)
                .list();
    }

    private static Follower mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Follower(
                URI.create(rs.getString("actor_id")),
                URI.create(rs.getString("inbox")),
                URI.create(rs.getString("follow_activity_id")),
                rs.getObject("followed_at", OffsetDateTime.class).toInstant());
    }
}
