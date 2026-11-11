package com.yue.jastodon.federation.actor;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UpdatePersonTest {

    private static final UUID UPDATE_ID = UUID.fromString("3f2b8c1d-6a4e-4f9b-8d2c-7e5a1b9c0d34");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final Actor person = new Actor(
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
            null);

    @Test
    void serialisesLikeMastodonUpdate() {
        String json = jsonMapper.writeValueAsString(UpdatePerson.of(person, UPDATE_ID));

        assertThat(jsonMapper.readTree(json)).isEqualTo(jsonMapper.readTree("""
                {
                  "@context": ["https://www.w3.org/ns/activitystreams", "https://w3id.org/security/v1"],
                  "id": "https://jastodon.test/users/yue#updates/3f2b8c1d-6a4e-4f9b-8d2c-7e5a1b9c0d34",
                  "type": "Update",
                  "actor": "https://jastodon.test/users/yue",
                  "to": ["https://www.w3.org/ns/activitystreams#Public"],
                  "object": {
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
                }
                """));
    }

    @Test
    void keepsItsOwnContextWhenThePersonHasNone() {
        UpdatePerson update = UpdatePerson.of(person.withoutContext(), UPDATE_ID);

        assertThat(update.context()).containsExactly(
                "https://www.w3.org/ns/activitystreams",
                "https://w3id.org/security/v1");
    }
}
