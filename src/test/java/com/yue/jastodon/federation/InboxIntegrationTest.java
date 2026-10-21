package com.yue.jastodon.federation;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.TestKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class InboxIntegrationTest extends AbstractIntegrationTest {

    private static final String INBOX = "/users/yue/inbox";
    private static final URI REMOTE_ACTOR = URI.create("https://remote.test/users/alice");
    private static final URI REMOTE_KEY_ID = URI.create("https://remote.test/users/alice#main-key");
    private static final KeyPair REMOTE_KEY_PAIR = TestKeys.generateRsaKeyPair();

    @Autowired
    RestTestClient client;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    FollowerRepository followerRepository;

    @LocalServerPort
    int port;

    @MockitoBean
    RemoteActorKeyFetcher keyFetcher;

    @MockitoBean
    RemoteActorFetcher remoteActorFetcher;

    @BeforeEach
    void remoteKeyIsKnown() {
        jdbcClient.sql("DELETE FROM followers").update();
        when(keyFetcher.fetch(REMOTE_KEY_ID))
                .thenReturn(new RemoteKey(REMOTE_KEY_ID, REMOTE_ACTOR, REMOTE_KEY_PAIR.getPublic()));
        when(remoteActorFetcher.fetch(REMOTE_ACTOR))
                .thenReturn(new RemoteActor(REMOTE_ACTOR, URI.create("https://remote.test/users/alice/inbox")));
    }

    private static HttpSignatureSigner remoteSigner() {
        FederationProperties remote = new FederationProperties(
                "alice",
                URI.create("https://remote.test"),
                new ByteArrayResource(TestKeys.toPem(REMOTE_KEY_PAIR.getPrivate()).getBytes(StandardCharsets.UTF_8)));
        return new HttpSignatureSigner(new ActorKeys(remote), remote, Clock.systemUTC());
    }

    @Test
    void acceptsCorrectlySignedActivity() {
        byte[] body = followJson().getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = remoteSigner().sign(
                HttpMethod.POST, URI.create("https://localhost:" + port + INBOX), body);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(body)
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void rejectsUnsignedActivity() {
        client.post().uri(INBOX)
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body("{\"type\":\"Follow\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsTamperedBody() {
        byte[] signedBody = followJson().getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = remoteSigner().sign(
                HttpMethod.POST, URI.create("http://localhost:" + port + INBOX), signedBody);
        byte[] tamperedBody = followJson().replace("\"Follow\"", "\"Delete\"")
                .getBytes(StandardCharsets.UTF_8);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(tamperedBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsWhenKeyCannotBeFetched() {
        when(keyFetcher.fetch(REMOTE_KEY_ID)).thenThrow(new RemoteFetchException("unreachable"));
        byte[] body = followJson().getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = remoteSigner().sign(
                HttpMethod.POST, URI.create("https://localhost:" + port + INBOX), body);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(body)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsActivityFromAnotherActor() {
        byte[] body = """
                {
                  "id": "https://remote.test/activities/follow-2",
                  "type": "Follow",
                  "actor": "https://remote.test/users/bob",
                  "object": "https://jastodon.test/users/yue"
                }
                """.getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = remoteSigner().sign(
                HttpMethod.POST, URI.create("http://localhost:" + port + INBOX), body);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(body)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsMalformedJson() {
        byte[] body = "not json".getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = remoteSigner().sign(
                HttpMethod.POST, URI.create("http://localhost:" + port + INBOX), body);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(body)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsActivityWithoutActor() {
        byte[] body = """
                {
                  "id": "https://remote.test/activities/follow-3",
                  "type": "Follow",
                  "object": "https://jastodon.test/users/yue"
                }
                """.getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = remoteSigner().sign(
                HttpMethod.POST, URI.create("http://localhost:" + port + INBOX), body);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(body)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsSignatureMadeWithAnotherKey() {
        KeyPair attacker = TestKeys.generateRsaKeyPair();
        FederationProperties forged = new FederationProperties(
                "alice",
                URI.create("https://remote.test"),
                new ByteArrayResource(TestKeys.toPem(attacker.getPrivate()).getBytes(StandardCharsets.UTF_8)));
        HttpSignatureSigner attackerSigner = new HttpSignatureSigner(new ActorKeys(forged), forged, Clock.systemUTC());

        byte[] body = followJson().getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = attackerSigner.sign(
                HttpMethod.POST, URI.create("http://localhost:" + port + INBOX), body);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(body)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void signedFollowStoresTheFollower() {
        byte[] body = followJson().getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = remoteSigner().sign(
                HttpMethod.POST, URI.create("http://localhost:" + port + INBOX), body);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(body)
                .exchange()
                .expectStatus().isAccepted();

        List<Follower> followers = followerRepository.findAll();
        assertThat(followers).hasSize(1);
        Follower follower = followers.get(0);
        assertThat(follower.actorId()).isEqualTo(REMOTE_ACTOR);
        assertThat(follower.inbox()).isEqualTo(URI.create("https://remote.test/users/alice/inbox"));

        assertThat(follower.followActivityId()).isEqualTo(URI.create("https://remote.test/activities/follow-1"));
    }

    @Test
    void answers503WhenTheFollowerCannotBeFetched() {
        when(remoteActorFetcher.fetch(REMOTE_ACTOR)).thenThrow(new RemoteFetchException("Unreachable"));
        byte[] body = followJson().getBytes(StandardCharsets.UTF_8);
        SignatureHeaders headers = remoteSigner().sign(
                HttpMethod.POST, URI.create("http://localhost:" + port + INBOX), body);

        client.post().uri(INBOX)
                .header("Date", headers.date())
                .header("Digest", headers.digest())
                .header("Signature", headers.signature())
                .contentType(MediaType.parseMediaType("application/activity+json"))
                .body(body)
                .exchange()
                .expectStatus().isEqualTo(503);

        assertThat(followerRepository.findAll()).isEmpty();
    }

    private static String followJson() {
        return """
                {
                  "id": "https://remote.test/activities/follow-1",
                  "type": "Follow",
                  "actor": "https://remote.test/users/alice",
                  "object": "https://jastodon.test/users/yue"
                }
                """;
    }
}
