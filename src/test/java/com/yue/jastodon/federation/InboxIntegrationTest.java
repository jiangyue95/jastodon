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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Clock;

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

    @LocalServerPort
    int port;

    @MockitoBean
    RemoteActorKeyFetcher keyFetcher;

    @BeforeEach
    void remoteKeyIsKnown() {
        when(keyFetcher.fetch(REMOTE_KEY_ID))
                .thenReturn(new RemoteKey(REMOTE_KEY_ID, REMOTE_ACTOR, REMOTE_KEY_PAIR.getPublic()));
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
        when(keyFetcher.fetch(REMOTE_KEY_ID)).thenThrow(new RemoteKeyFetchException("unreachable"));
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
