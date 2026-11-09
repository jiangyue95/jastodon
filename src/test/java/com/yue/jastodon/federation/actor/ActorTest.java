package com.yue.jastodon.federation.actor;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActorTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final Actor actor = new Actor(
            List.of("https://www.w3.org/ns/activitystreams", "https://w3id.org/security/v1"),
            URI.create("https://jastodon.test/users/yue"),
            "Person",
            "yue",
            URI.create("https://jastodon.test/users/yue/inbox"),
            URI.create("https://jastodon.test/users/yue/outbox"),
            new Actor.PublicKey(
                    URI.create("https://jastodon.test/users/yue#main-key"),
                    URI.create("https://jastodon.test/users/yue"),
                    "test-pem"));

    @Test
    void embeddedFormOmitsTheContext() {
        String json = jsonMapper.writeValueAsString(actor.withoutContext());

        assertThat(jsonMapper.readTree(json)).isEqualTo(jsonMapper.readTree("""
                {
                  "id": "https://jastodon.test/users/yue",
                  "type": "Person",
                  "preferredUsername": "yue",
                  "inbox": "https://jastodon.test/users/yue/inbox",
                  "outbox": "https://jastodon.test/users/yue/outbox",
                  "publicKey": {
                    "id": "https://jastodon.test/users/yue#main-key",
                    "owner": "https://jastodon.test/users/yue",
                    "publicKeyPem": "test-pem"
                  }
                }
                """));
    }
}
