package com.yue.jastodon.federation;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ActorIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE_URL = "https://jastodon.test";
    private static final String ACTOR_ID = BASE_URL + "/users/yue";

    @Autowired
    RestTestClient client;

    @Test
    void returnsActorForLocalAccount() {
        client.get().uri("/users/yue")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/activity+json")
                .expectBody()
                .jsonPath("$['@context']").isEqualTo("https://www.w3.org/ns/activitystreams")
                .jsonPath("$.id").isEqualTo(ACTOR_ID)
                .jsonPath("$.type").isEqualTo("Person")
                .jsonPath("$.preferredUsername").isEqualTo("yue")
                .jsonPath("$.inbox").isEqualTo(ACTOR_ID + "/inbox")
                .jsonPath("$.outbox").isEqualTo(ACTOR_ID + "/outbox");
    }

    @Test
    void actorIdMatchesWebFingerHref() {
        Jrd jrd = client.get().uri("/.well-known/webfinger?resource={r}", "acct:yue@jastodon.test")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Jrd.class)
                .returnResult()
                .getResponseBody();

        Actor actor = client.get().uri("/users/yue")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Actor.class)
                .returnResult().getResponseBody();

        assertThat(actor.id().toString()).isEqualTo(jrd.links().get(0).href());
    }

    @Test
    void otherUsersAreNotPublic() {
        client.get().uri("/users/alice")
                .exchange()
                .expectStatus().isFound();
    }

    @Test
    void subPathsAreNotPublic() {
        client.get().uri("/users/yue/inbox")
                .exchange()
                .expectStatus().isFound();
    }
}
