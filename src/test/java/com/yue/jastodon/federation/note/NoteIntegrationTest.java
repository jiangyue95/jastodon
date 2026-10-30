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
class NoteIntegrationTest extends AbstractIntegrationTest {

    private static final UUID ID = UUID.fromString("0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");

    @Autowired
    RestTestClient client;

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void storeOneNote() {
        jdbcClient.sql("DELETE FROM notes").update();
        noteRepository.save(new Note(ID, "a < b\nc", Instant.parse("2026-10-07T10:00:00Z")));
    }

    @Test
    void servesAStoredNote() {
        client.get().uri("/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/activity+json")
                .expectBody()
                .jsonPath("$['@context']").isEqualTo("https://www.w3.org/ns/activitystreams")
                .jsonPath("$.id").isEqualTo(
                        "https://jastodon.test/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69")
                .jsonPath("$.type").isEqualTo("Note")
                .jsonPath("$.attributedTo").isEqualTo("https://jastodon.test/users/yue")
                .jsonPath("$.content").isEqualTo("<p>a &lt; b<br />c</p>")
                .jsonPath("$.published").isEqualTo("2026-10-07T10:00:00Z")
                .jsonPath("$.to[0]").isEqualTo("https://www.w3.org/ns/activitystreams#Public");
    }

    @Test
    void returnsNotFoundForAnUnknownId() {
        client.get().uri("/users/yue/statuses/1e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returnsNotFoundForANonCanonicalId() {
        client.get().uri("/users/yue/statuses/0E1F6A3C-5B2D-4C8E-9A7F-1D2E3C4B5A69")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returnsNotFoundForAnIdThatIsNotAUuid() {
        client.get().uri("/users/yue/statuses/not-a-uuid")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void notesOfOtherUsersAreNotPublic() {
        client.get().uri("/users/alice/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69")
                .exchange()
                .expectStatus().isFound();
    }

    @Test
    void subPathsOfANoteAreNotPublic() {
        client.get().uri("/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69/edit")
                .exchange()
                .expectStatus().isFound();
    }
}
