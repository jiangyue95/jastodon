package com.yue.jastodon.federation.actor;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActorTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final Actor actor = actorWith(null);

    @Test
    void embeddedFormKeepsTheIcon() {
        Actor.Icon icon = new Actor.Icon(
                "Image",
                "image/png",
                URI.create("https://media.jastodon.test/avatars/6f1c2a3e-8b4d-4e7a-9c5f-2d3e4f5a6b7c.png"));

        assertThat(actorWith(icon).withoutContext().icon()).isEqualTo(icon);
    }

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

    private static Actor actorWith(Actor.Icon icon) {
        return new Actor(
                List.of("https://www.w3.org/ns/activitystreams", "https://w3id.org/security/v1"),
                URI.create("https://jastodon.test/users/yue"),
                "Person",
                "yue",
                URI.create("https://jastodon.test/users/yue/inbox"),
                URI.create("https://jastodon.test/users/yue/outbox"),
                new Actor.PublicKey(
                        URI.create("https://jastodon.test/users/yue#main-key"),
                        URI.create("https://jastodon.test/users/yue"),
                        "test-pem"),
                icon);
    }
}
