package com.yue.jastodon.federation.note;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import com.yue.jastodon.federation.follower.Follower;
import com.yue.jastodon.federation.follower.FollowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NoteServiceTest extends AbstractIntegrationTest {

    private static final UUID ID = UUID.fromString("0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-07T10:00:00.123456789Z"), ZoneOffset.UTC);

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    FollowerRepository followerRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    FederationProperties federationProperties;

    @Autowired
    JsonMapper jsonMapper;

    NoteService noteService;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM notes").update();
        jdbcClient.sql("DELETE FROM followers").update();
        jdbcClient.sql("DELETE FROM deliveries").update();
        noteService = new NoteService(
                noteRepository,
                () -> ID,
                CLOCK,
                followerRepository,
                deliveryRepository,
                federationProperties,
                jsonMapper);
    }

    @Test
    void createReturnsTheStoredNote() {
        Note created = noteService.create("hello");

        assertThat(created).isEqualTo(new Note(
                ID,
                "hello",
                Instant.parse("2026-10-07T10:00:00.123456Z")));
        assertThat(noteRepository.findAll()).containsExactly(created);
    }

    @Test
    void rejectsBlankContentAndStoresNothing() {
        assertThatThrownBy(() -> noteService.create("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Note content must not be blank");

        assertThat(noteRepository.findAll()).isEmpty();
    }

    @Test
    void enqueuesTheCreateForEveryFollower() {
        followerRepository.save(follower("alice"));
        followerRepository.save(follower("bob"));

        noteService.create("hello");

        assertThat(jdbcClient.sql("SELECT inbox FROM deliveries ORDER BY id").query(String.class).list())
                .containsExactly(
                        "https://remote.test/users/alice/inbox",
                        "https://remote.test/users/bob/inbox");
        assertThat(jdbcClient.sql("SELECT body FROM deliveries").query(String.class).list())
                .allSatisfy(body -> assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                        {
                          "@context": "https://www.w3.org/ns/activitystreams#public",
                          "id": "https://jastodon.test/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69/activity",
                          "type": "Create",
                          "actor": "https://jastodon.test/users/yue",
                          "published": "2026-10-07T10:00:00.123456Z",
                          "to": ["https://www.w3.org/ns/activitystreams#public"],
                          "object": {
                            "id": "https://jastodon.test/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69",
                            "type": "Note",
                            "attributedTo": "https://jastodon.test/users/yue",
                            "content": "<p>hello</p>",
                            "published": "2026-10-07T10:00:00.123456Z",
                            "to": ["https://www.w3.org/ns/activitystreams#public"]
                          }
                        }
                        """)));
    }

    private static Follower follower(String name) {
        return new Follower(
                URI.create("https://remote.test/users/" + name),
                URI.create("https://remote.test/users/" + name + "/inbox"),
                URI.create("https://remote.test/follows/" + name),
                Instant.parse("2026-10-07T09:00:00Z"));
    }
}
