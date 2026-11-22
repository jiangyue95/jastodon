package com.yue.jastodon.federation.remote;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteActorDocumentTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void readsTheName() {
        assertThat(read("""
                {"id": "https://remote.example/users/alice", "name": "Alice"}
                """).displayName()).isEqualTo("Alice");
    }

    @Test
    void hasNoNameWhenItIsMissing() {
        assertThat(read("""
                {"id": "https://remote.example/users/alice"}
                """).displayName()).isNull();
    }

    @Test
    void hasNoNameWhenItIsNotAString() {
        assertThat(read("""
                {"id": "https://remote.example/users/alice", "name": {"en": "Alice"}}
                """).displayName()).isNull();
    }

    @Test
    void hasNoNameWhenItIsNull() {
        assertThat(read("""
                {"id": "https://remote.example/users/alice", "name": null}
                """).displayName()).isNull();
    }

    @Test
    void readsTheAvatarFromAnImageObject() {
        assertThat(read("""
                {"id": "https://remote.example/users/alice",
                 "icon": {"type": "Image", "url": "https://files.remote.example/alice.png"}}
                """).avatarUrl()).isEqualTo(URI.create("https://files.remote.example/alice.png"));
    }

    @Test
    void readsTheAvatarFromTheFirstImageInAnArray() {
        assertThat(read("""
                {
                  "id": "https://remote.example/users/alice",
                  "icon": [
                    {"type": "Image", "url": "https://files.remote.example/alice-1.png"},
                    {"type": "Image", "url": "https://files.remote.example/alice-2.png"}
                  ]
                }
                """).avatarUrl()).isEqualTo(URI.create("https://files.remote.example/alice-1.png"));
    }

    @Test
    void readsTheAvatarFromALinkObject() {
        assertThat(read("""
                {
                  "id": "https://remote.example/users/alice",
                  "icon": {
                    "type": "Image",
                    "url": {
                      "type": "Link",
                      "href": "https://files.remote.example/alice.png"
                    }
                  }
                }
                """).avatarUrl()).isEqualTo(URI.create("https://files.remote.example/alice.png"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"https://files.remote.example/alice.png\"",
            "[]",
            "{\"type\": \"Image\"}",
            "{\"type\": \"Image\", \"url\": 42}",
            "{\"type\": \"Image\", \"url\": \"http://files.remote.example/alice.png\"}",
            "{\"type\": \"Image\", \"url\": \"https://files.remote.example/a b.png\"}",
            "{\"type\": \"Image\", \"url\": \"https:files.remote.example/alice.png\"}"
    })
    void hasNoAvatarForAnUnusableIcon(String icon) {
        assertThat(read("""
                {"id": "https://remote.example/users/alice", "icon": %s}
                """.formatted(icon)).avatarUrl()).isNull();
    }

    @Test
    void hasNoAvatarWhenThereIsNoIcon() {
        assertThat(read("""
                {"id": "https://remote.example/users/alice"}
                """).avatarUrl()).isNull();
    }

    private RemoteActorDocument read(String json) {
        return jsonMapper.readValue(json, RemoteActorDocument.class);
    }
}
