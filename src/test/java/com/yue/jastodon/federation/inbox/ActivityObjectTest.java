package com.yue.jastodon.federation.inbox;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityObjectTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Test
    void readsAReference() {
        ActivityObject object = JSON_MAPPER.readValue(
                "\"https://jastodon.test/users/yue\"",
                ActivityObject.class);

        assertThat(object).isEqualTo(new ActivityObject(
                URI.create("https://jastodon.test/users/yue"),
                null,
                null,
                null));
    }

    @Test
    void readsAnEmbeddedObject() {
        ActivityObject object = JSON_MAPPER.readValue("""
                {
                  "id": "https://remote.test/users/alice#follows/1",
                  "type": "Follow",
                  "actor": "https://remote.test/users/alice",
                  "object": "https://jastodon.test/users/yue",
                  "unknown": true
                }
                """, ActivityObject.class);

        assertThat(object).isEqualTo(new ActivityObject(
                URI.create("https://remote.test/users/alice#follows/1"),
                "Follow",
                URI.create("https://remote.test/users/alice"),
                URI.create("https://jastodon.test/users/yue")));
    }
}
