package com.yue.jastodon.federation.remote;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebFingerClientTest {

    private static final Acct ALICE_ACCT = new Acct("alice", "mastodon.social");
    private static final URI WEBFINGER_URL = URI.create(
            "https://mastodon.social/.well-known/webfinger?resource=acct:alice@mastodon.social");
    private static final URI ALICE = URI.create("https://mastodon.social/users/alice");
    private static final String ACTIVITY_JSON = "application/activity+json";

    private final ActivityPubClient activityPubClient = mock(ActivityPubClient.class);
    private final WebFingerClient webFingerClient = new WebFingerClient(activityPubClient);

    @Test
    void resolvesTheActorUrlFromTheSelfLink() {
        respondWith(new WebFingerDocument(
                "acct:alice@mastodon.social",
                List.of(new WebFingerDocument.Link("self", "application/activity+json", ALICE))));

        assertThat(webFingerClient.resolve(ALICE_ACCT)).isEqualTo(new WebFingerAccount(ALICE_ACCT, ALICE));
    }

    @Test
    void usesTheFirstActivityPubSelfLink() {
        respondWith(new WebFingerDocument(
                "acct:alice@mastodon.social",
                List.of(
                        selfLink("text/html", URI.create("https://mastodon.social/@alice")),
                        new WebFingerDocument.Link(
                                "alternate",
                                ACTIVITY_JSON,
                                URI.create("https://mastodon.social/users/bob")),
                        selfLink(ACTIVITY_JSON, ALICE),
                        selfLink(ACTIVITY_JSON, URI.create("https://mastodon.social/users/carol"))
                )
        ));

        assertThat(webFingerClient.resolve(ALICE_ACCT)).isEqualTo(new WebFingerAccount(ALICE_ACCT, ALICE));
    }

    @Test
    void rejectsAnotherAccountsSubject() {
        respondWith(new WebFingerDocument(
                "acct:bob@mastodon.social",
                List.of(selfLink(ACTIVITY_JSON, ALICE))));

        assertThatThrownBy(() -> webFingerClient.resolve(ALICE_ACCT))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessage("WebFinger subject acct:bob@mastodon.social does not match acct:alice@mastodon.social");
    }

    @Test
    void acceptsTheLdJsonActivityStreamsType() {
        respondWith(new WebFingerDocument(
                "acct:alice@mastodon.social",
                List.of(selfLink(
                        "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"",
                        ALICE))));

        assertThat(webFingerClient.resolve(ALICE_ACCT)).isEqualTo(new WebFingerAccount(ALICE_ACCT, ALICE));
    }

    @Test
    void usesTheSubjectsSpellingOfTheHandle() {
        respondWith(new WebFingerDocument(
                "acct:Alice@Mastodon.Social",
                List.of(selfLink(ACTIVITY_JSON, ALICE))));

        assertThat(webFingerClient.resolve(ALICE_ACCT))
                .isEqualTo(new WebFingerAccount(new Acct("Alice", "mastodon.social"), ALICE));
    }

    @Test
    void skipsNullLinks() {
        respondWith(new WebFingerDocument(
                "acct:alice@mastodon.social",
                Arrays.asList(null, selfLink(ACTIVITY_JSON, ALICE))));

        assertThat(webFingerClient.resolve(ALICE_ACCT)).isEqualTo(new WebFingerAccount(ALICE_ACCT, ALICE));
    }

    @Test
    void rejectsADocumentWithoutSubject() {
        respondWith(new WebFingerDocument(
                null,
                List.of(selfLink(ACTIVITY_JSON, ALICE))));

        assertThatThrownBy(() -> webFingerClient.resolve(ALICE_ACCT))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessage("WebFinger subject null does not match acct:alice@mastodon.social");
    }

    @Test
    void rejectsADocumentWithoutLinks() {
        respondWith(new WebFingerDocument(
                "acct:alice@mastodon.social",
                null));

        assertThatThrownBy(() -> webFingerClient.resolve(ALICE_ACCT))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessage("WebFinger response for acct:alice@mastodon.social has no ActivityPub self link");
    }

    @Test
    void rejectsADocumentWithoutAnActivityPubSelfLink() {
        respondWith(new WebFingerDocument(
                "acct:alice@mastodon.social",
                List.of(
                        selfLink("text/html", ALICE),
                        selfLink("application/json", ALICE),
                        selfLink(null, ALICE))));

        assertThatThrownBy(() -> webFingerClient.resolve(ALICE_ACCT))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessage("WebFinger response for acct:alice@mastodon.social has no ActivityPub self link");
    }

    @Test
    void rejectsASelfLinkWithoutHref() {
        respondWith(new WebFingerDocument(
                "acct:alice@mastodon.social",
                List.of(selfLink(ACTIVITY_JSON, null))));

        assertThatThrownBy(() -> webFingerClient.resolve(ALICE_ACCT))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessage("WebFinger self link for acct:alice@mastodon.social has no href");
    }

    @Test
    void rejectsAnEmptyResponse() {
        respondWith(null);

        assertThatThrownBy(() -> webFingerClient.resolve(ALICE_ACCT))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessage("WebFinger returned an empty response for acct:alice@mastodon.social");
    }

    @Test
    void passesOnAFetchFailure() {
        RemoteFetchException failure = new RemoteFetchException("Cannot fetch " + WEBFINGER_URL);
        when(activityPubClient.getUnsigned(
                WEBFINGER_URL,
                WebFingerDocument.class,
                MediaType.parseMediaType("application/jrd+json"),
                MediaType.APPLICATION_JSON))
                .thenThrow(failure);

        assertThatThrownBy(() -> webFingerClient.resolve(ALICE_ACCT))
                .isSameAs(failure);
    }

    private void respondWith(WebFingerDocument document) {
        when(activityPubClient.getUnsigned(
                WEBFINGER_URL,
                WebFingerDocument.class,
                MediaType.parseMediaType("application/jrd+json"),
                MediaType.APPLICATION_JSON))
                .thenReturn(document);
    }

    private static WebFingerDocument.Link selfLink(String type, URI href) {
        return new WebFingerDocument.Link("self", type, href);
    }
}
