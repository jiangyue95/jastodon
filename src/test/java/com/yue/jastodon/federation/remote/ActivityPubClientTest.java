package com.yue.jastodon.federation.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import com.yue.jastodon.TestKeys;
import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.actor.ActorKeys;
import com.yue.jastodon.federation.signature.HttpSignatureSigner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityPubClientTest {

    private static final KeyPair KEY_PAIR = TestKeys.generateRsaKeyPair();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC);
    private static final byte[] BODY = "{\"type\":\"Accept\"}".getBytes(StandardCharsets.UTF_8);

    private final AtomicInteger requestCount = new AtomicInteger();
    private volatile String lastMethod;
    private volatile Headers lastHeaders;
    private volatile byte[] lastBody;
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void postsSignedActivityAndReturnsTheStatus() {
        respondWith(202);

        HttpStatusCode status = client(uri -> {}).post(inbox(), BODY);

        assertThat(status.value()).isEqualTo(202);
        assertThat(requestCount).hasValue(1);
        assertThat(lastMethod).isEqualTo("POST");
        assertThat(lastHeaders.getFirst("Content-Type")).isEqualTo("application/activity+json");
        assertThat(lastHeaders.getFirst("Date")).isEqualTo("Wed, 07 Oct 2026 10:00:00 GMT");
        assertThat(lastHeaders.getFirst("Digest"))
                .isEqualTo("SHA-256=HWKqeTJnS/L2x7rjx755Vv5ri6Ld95yKxd3htgM0d6Y=");
        assertThat(lastHeaders.getFirst("Signature"))
                .contains("keyId=\"https://jastodon.test/users/yue#main-key\"");
    }

    @Test
    void returnsAnErrorStatusInsteadOfThrowing() {
        respondWith(503);

        HttpStatusCode status = client(uri -> {}).post(inbox(), BODY);

        assertThat(status.value()).isEqualTo(503);
    }

    @Test
    void refusesUnsafeInboxWithoutSendingAnything() {
        respondWith(202);
        OutboundUrlPolicy rejectEverything = uri -> {
            throw new UnsafeRemoteUrlException("blocked by test policy");
        };

        assertThatThrownBy(() -> client(rejectEverything).post(inbox(), BODY))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("blocked by test policy");
        assertThat(requestCount).hasValue(0);
    }

    @Test
    void wrapsConnectionFailures() {
        URI nothingListening = URI.create("http://127.0.0.1:1/inbox");

        assertThatThrownBy(() -> client(uri -> {}).post(nothingListening, BODY))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessageContaining("Cannot deliver");
    }

    private void respondWith(int status) {
        server.createContext("/inbox", exchange -> {
            requestCount.incrementAndGet();
            lastMethod = exchange.getRequestMethod();
            lastHeaders = exchange.getRequestHeaders();
            lastBody = exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
    }

    private URI inbox() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/inbox");
    }

    private static ActivityPubClient client(OutboundUrlPolicy policy) {
        FederationProperties properties = new FederationProperties(
                "yue",
                URI.create("https://jastodon.test"),
                new ByteArrayResource(TestKeys.toPem(KEY_PAIR.getPrivate()).getBytes(StandardCharsets.UTF_8)));
        HttpSignatureSigner signer = new HttpSignatureSigner(new ActorKeys(properties), properties, CLOCK);
        return new ActivityPubClient(new FederationClientConfiguration().federationRestClient(), policy, signer);
    }
}
