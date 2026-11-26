package com.yue.jastodon.federation.following;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FollowActivityTest {

    private static final URI YUE = URI.create("https://jastodon.test/users/yue");
    private static final URI ALICE = URI.create("https://remote.test/users/alice");
    private static final UUID FOLLOW_UUID = UUID.fromString("0bbb2b13-e276-4929-90f8-7004fa3d6fe6");

    @Test
    void serialisesLikeMastodonFollow() {
        JsonMapper jsonMapper = JsonMapper.builder().build();

        String json = jsonMapper.writeValueAsString(FollowActivity.of(YUE, FOLLOW_UUID, ALICE));

        assertThat(jsonMapper.readTree(json)).isEqualTo(jsonMapper.readTree("""
                {
                  "@context": "https://www.w3.org/ns/activitystreams",
                  "id": "https://jastodon.test/users/yue#follows/0bbb2b13-e276-4929-90f8-7004fa3d6fe6",
                  "type": "Follow",
                  "actor": "https://jastodon.test/users/yue",
                  "object": "https://remote.test/users/alice"
                }
                """));
    }
}
