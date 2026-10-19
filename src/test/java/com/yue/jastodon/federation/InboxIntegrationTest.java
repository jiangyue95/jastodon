package com.yue.jastodon.federation;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class InboxIntegrationTest extends AbstractIntegrationTest {

    private static final String INBOX = "/users/yue/inbox";

    @Autowired
    RestTestClient client;

    @Test
    void rejectsUnsignedActivity() {
        client.post().uri(INBOX)
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body("{\"type\":\"Follow\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
