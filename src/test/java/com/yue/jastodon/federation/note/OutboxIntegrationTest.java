package com.yue.jastodon.federation.note;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class OutboxIntegrationTest extends AbstractIntegrationTest {

    private static final UUID OLDER_ID = UUID.fromString("0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");
    private static final UUID NEWER_ID = UUID.fromString("1e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");

    @Autowired
    RestTestClient client;

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void storeTwoNotes() {
        jdbcClient.sql("DELETE FROM notes").update();
        noteRepository.save(new Note(OLDER_ID, "older", Instant.parse("2026-10-07T10:00:00Z")));
        noteRepository.save(new Note(NEWER_ID, "newer", Instant.parse("2026-10-07T11:00:00Z")));
    }

    @Test
    void servesTheOutboxNewestFirst() {
        client.get().uri("/users/yue/outbox")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/activity+json")
                .expectBody()
                .jsonPath("$['@context']").isEqualTo("https://www.w3.org/ns/activitystreams")
                .jsonPath("$.id").isEqualTo("https://jastodon.test/users/yue/outbox")
                .jsonPath("$.type").isEqualTo("OrderedCollection")
                .jsonPath("$.totalItems").isEqualTo(2)
                .jsonPath("$.orderedItems[0].id").isEqualTo("https://jastodon.test/users/yue/statuses/1e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69/activity")
                .jsonPath("$.orderedItems[0].type").isEqualTo("Create")
                .jsonPath("$.orderedItems[0].object.content").isEqualTo("<p>newer</p>")
                .jsonPath("$.orderedItems[1].id").isEqualTo("https://jastodon.test/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69/activity");
    }

    @Test
    void outboxesOfOtherUsersAreNotPublic() {
        client.get().uri("/users/alice/outbox")
                .exchange()
                .expectStatus().is3xxRedirection();
    }
}
