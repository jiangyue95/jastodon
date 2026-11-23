package com.yue.jastodon.federation.remote;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebFingerClientTest {

    private static final Acct ALICE_ACCT = new Acct("alice", "mastodon.social");
    private static final URI WEBFINGER_URL = URI.create(
            "https://mastodon.social/.well-known/webfinger?resource=acct:alice@mastodon.social");
    private static final URI ALICE = URI.create("https://mastodon.social/users/alice");

    private final ActivityPubClient activityPubClient = mock(ActivityPubClient.class);
    private final WebFingerClient webFingerClient = new WebFingerClient(activityPubClient);

    @Test
    void resolvesTheActorUrlFromTheSelfLink() {
        respondWith(new WebFingerDocument(
                "acct:alice@mastodon.social",
                List.of(new WebFingerDocument.Link("self", "application/activity+json", ALICE))));

        assertThat(webFingerClient.resolve(ALICE_ACCT)).isEqualTo(ALICE);
    }

    private void respondWith(WebFingerDocument document) {
        when(activityPubClient.get(
                WEBFINGER_URL,
                WebFingerDocument.class,
                MediaType.parseMediaType("application/jrd+json"),
                MediaType.APPLICATION_JSON))
                .thenReturn(document);
    }
}
