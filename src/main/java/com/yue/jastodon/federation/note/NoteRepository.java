package com.yue.jastodon.federation.note;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class NoteRepository {

    private final JdbcClient jdbcClient;

    NoteRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void save(Note note) {
        jdbcClient.sql("""
                       INSERT INTO notes (id, content, published)
                       VALUES (:id, :content, :published)
                       """)
                .param("id", note.id())
                .param("content", note.content())
                .param("published", note.published().atOffset(ZoneOffset.UTC))
                .update();
    }

    List<Note> findAll() {
        return jdbcClient.sql("""
                              SELECT id, content, published
                              FROM notes
                              ORDER BY published DESC, id
                              """)
                .query(NoteRepository::mapRow)
                .list();
    }

    Optional<Note> findById(UUID id) {
        return null;
    }

    private static Note mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Note(
                rs.getObject("id", UUID.class),
                rs.getString("content"),
                rs.getObject("published", OffsetDateTime.class).toInstant());
    }
}
