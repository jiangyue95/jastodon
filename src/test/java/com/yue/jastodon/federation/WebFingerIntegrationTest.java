package com.yue.jastodon.federation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class WebFingerIntegrationTest {

    private static final String WEBFINGER_PATH = "/.well-known/webfinger";
    private static final String WEBFINGER = WEBFINGER_PATH + "?resource={resource}";

    @Autowired
    RestTestClient client;

    @Test
    void returnsJrdForLocalAccount() {
        client.get().uri(WEBFINGER, "acct:yue@jastodon.test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/jrd+json")
                .expectBody()
                .jsonPath("$.subject").isEqualTo("acct:yue@jastodon.test")
                .jsonPath("$.links[0].rel").isEqualTo("self")
                .jsonPath("$.links[0].type").isEqualTo("application/activity+json")
                .jsonPath("$.links[0].href").isEqualTo("https://jastodon.test/users/yue");
    }

    @Test
    void normalisesSubjectCase() {
        client.get().uri(WEBFINGER, "acct:YUE@Jastodon.TEST")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.subject").isEqualTo("acct:yue@jastodon.test");
    }

    @Test
    void returns404ForUnknownAccount() {
        client.get().uri(WEBFINGER, "acct:alice@jastodon.test")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returns400ForMalformedAcct() {
        client.get().uri(WEBFINGER, "acct:yue")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void returns400WhenResourceIsBlank() {
        client.get().uri(WEBFINGER, "")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void returns400WhenResourceIsMissing() {
        client.get().uri(WEBFINGER_PATH)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
