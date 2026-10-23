package com.yue.jastodon.federation.delivery;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class DeliveryRepository {

    private final JdbcClient jdbcClient;

    public DeliveryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long enqueue(URI inbox, String body, Instant now) {
        return jdbcClient.sql("""
                       INSERT INTO deliveries (inbox, body, status, next_attempt_at, created_at)
                       VALUES (:inbox, :body, :status, :now, :now)
                       RETURNING id
                       """)
                .param("inbox", inbox.toString())
                .param("body", body)
                .param("status", DeliveryStatus.PENDING.name())
                .param("now", now.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    List<Delivery> findDue(Instant now) {
        return jdbcClient.sql("""
                       SELECT id, inbox, body, status, attempts, next_attempt_at, last_error, created_at
                       FROM deliveries
                       WHERE status = 'PENDING'
                         AND next_attempt_at <= :now
                       ORDER BY next_attempt_at, id
                       """)
                .param("now", now.atOffset(ZoneOffset.UTC))
                .query(DeliveryRepository::mapRow)
                .list();
    }

    private static Delivery mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Delivery(
                rs.getLong("id"),
                URI.create(rs.getString("inbox")),
                rs.getString("body"),
                DeliveryStatus.valueOf(rs.getString("status")),
                rs.getInt("attempts"),
                rs.getObject("next_attempt_at", OffsetDateTime.class).toInstant(),
                rs.getString("last_error"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
