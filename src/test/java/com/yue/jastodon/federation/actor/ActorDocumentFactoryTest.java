package com.yue.jastodon.federation.actor;

import com.yue.jastodon.TestKeys;
import com.yue.jastodon.federation.FederationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ActorDocumentFactoryTest {

    private final FederationProperties properties = new FederationProperties(
            "yue",
            URI.create("https://jastodon.test"),
            new ByteArrayResource(TestKeys.toPem(TestKeys.generateRsaKeyPair().getPrivate())
                    .getBytes(StandardCharsets.UTF_8)));
    private final ActorKeys actorKeys = new ActorKeys(properties);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void buildsTheActorDocument() {
        String json = jsonMapper.writeValueAsString(new ActorDocumentFactory(properties, actorKeys).build());

        assertThat(jsonMapper.readTree(json)).isEqualTo(jsonMapper.readTree("""
                {
                  "@context": [
                    "https://www.w3.org/ns/activitystreams",
                    "https://w3id.org/security/v1"
                  ],
                  "id": "https://jastodon.test/users/yue",
                  "type": "Person",
                  "preferredUsername": "yue",
                  "inbox": "https://jastodon.test/users/yue/inbox",
                  "outbox": "https://jastodon.test/users/yue/outbox",
                  "publicKey": {
                    "id": "https://jastodon.test/users/yue#main-key",
                    "owner": "https://jastodon.test/users/yue",
                    "publicKeyPem": %s
                  }
                }
                """.formatted(jsonMapper.writeValueAsString(actorKeys.publicKeyPem()))));
    }
}
