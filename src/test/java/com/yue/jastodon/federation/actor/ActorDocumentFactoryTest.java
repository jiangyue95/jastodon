package com.yue.jastodon.federation.actor;

import com.yue.jastodon.TestKeys;
import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.profile.Avatars;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActorDocumentFactoryTest {

    private final FederationProperties properties = new FederationProperties(
            "yue",
            URI.create("https://jastodon.test"),
            new ByteArrayResource(TestKeys.toPem(TestKeys.generateRsaKeyPair().getPrivate())
                    .getBytes(StandardCharsets.UTF_8)));
    private final ActorKeys actorKeys = new ActorKeys(properties);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final Avatars avatars = mock(Avatars.class);

    @Test
    void buildsTheActorDocument() {
        when(avatars.currentUrl("yue")).thenReturn(Optional.empty());

        String json = jsonMapper.writeValueAsString(new ActorDocumentFactory(properties, actorKeys, avatars).build());

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

    @Test
    void includesTheIconWhenThereIsAnAvatar() {
        when(avatars.currentUrl("yue")).thenReturn(Optional.of(
                URI.create("https://media.jastodon.test/avatars/6f1c2a3e-8b4d-4e7a-9c5f-2d3e4f5a6b7c.png")));

        String json = jsonMapper.writeValueAsString(new ActorDocumentFactory(properties, actorKeys, avatars).build());

        assertThat(jsonMapper.readTree(json).get("icon")).isEqualTo(jsonMapper.readTree("""
                {
                  "type": "Image",
                  "mediaType": "image/png",
                  "url": "https://media.jastodon.test/avatars/6f1c2a3e-8b4d-4e7a-9c5f-2d3e4f5a6b7c.png"
                }
                """));
    }
}
