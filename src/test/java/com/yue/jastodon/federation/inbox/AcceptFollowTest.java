package com.yue.jastodon.federation.inbox;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptFollowTest {

    private static final URI YUE = URI.create("https://jastodon.test/users/yue");
    private static final URI ALICE = URI.create("https://remote.test/users/alice");
    private static final URI FOLLOW_ID = URI.create("https://remote.test/activities/follow-1");
    // printf '%s' 'https://remote.test/activities/follow-1' | shasum -a 256
    private static final String FOLLOW_ID_SHA256 = "e534a88167429ce6497473a846acf3915cf9b6148afd215a9590a55078bbd9cb";

    @Test
    void idIsDerivedFromTheFollowId() {
        AcceptFollow accept = AcceptFollow.of(YUE, FOLLOW_ID, ALICE);

        assertThat(accept.id()).isEqualTo(URI.create("https://jastodon.test/users/yue#accepts/follows/"
                + FOLLOW_ID_SHA256));
    }

    @Test
    void serialisesLikeMastodonAccept() {
        JsonMapper jsonMapper = JsonMapper.builder().build();

        String json = jsonMapper.writeValueAsString(AcceptFollow.of(YUE, FOLLOW_ID, ALICE));

        assertThat(jsonMapper.readTree(json)).isEqualTo(jsonMapper.readTree("""
                {
                  "@context": "https://www.w3.org/ns/activitystreams",
                  "id": "https://jastodon.test/users/yue#accepts/follows/e534a88167429ce6497473a846acf3915cf9b6148afd215a9590a55078bbd9cb",
                  "type": "Accept",
                  "actor": "https://jastodon.test/users/yue",
                  "object": {
                    "id": "https://remote.test/activities/follow-1",
                    "type": "Follow",
                    "actor": "https://remote.test/users/alice",
                    "object": "https://jastodon.test/users/yue"
                  }
                }
                """));
    }
}
