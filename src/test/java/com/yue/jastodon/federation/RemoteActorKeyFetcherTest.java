package com.yue.jastodon.federation;

import com.sun.net.httpserver.HttpServer;
import com.yue.jastodon.TestKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteActorKeyFetcherTest {

    private static final KeyPair KEY_PAIR = TestKeys.generateRsaKeyPair();

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private volatile String lastAccept;
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
    void fetchesKeyFromActorDocument() {
        URI actor = url("/users/alice");
        URI keyId = URI.create(actor + "#main-key");
        serve("/users/alice", 200, Map.of("Content-Type", "application/activity+json"),
                actorJson(keyId, actor, TestKeys.toPem(KEY_PAIR.getPublic())));

        RemoteKey key = fetcherAllowingEverything().fetch(keyId);

        assertThat(key.keyId()).isEqualTo(keyId);
        assertThat(key.owner()).isEqualTo(actor);
        assertThat(key.publicKey().getEncoded()).isEqualTo(KEY_PAIR.getPublic().getEncoded());
        assertThat(lastAccept).contains("application/activity+json");
        assertThat(requestCounts.get("/users/alice")).hasValue(1);
    }

    @Test
    void rejectsOwnerOnAnotherHost() {
        URI actor = url("/users/alice");
        URI keyId = URI.create(actor + "#main-key");
        URI impersonated = URI.create("https://mastodon.social/users/bob");
        serve("/users/alice", 200, Map.of("Content-Type", "application/activity+json"),
                actorJson(keyId, impersonated, TestKeys.toPem(KEY_PAIR.getPublic())));

        assertThatThrownBy(() -> fetcherAllowingEverything().fetch(keyId))
                .isInstanceOf(RemoteKeyFetchException.class)
                .hasMessageContaining("not on the same host");
    }

    @Test
    void doesNotFollowRedirects() {
        URI actor = url("/users/alice");
        URI keyId = URI.create(actor + "#main-key");
        serve("/users/alice", 302, Map.of("Location", url("/internal").toString()), "");
        serve("/internal", 200, Map.of("Content-Type", "application/activity+json"),
                actorJson(keyId, actor, TestKeys.toPem(KEY_PAIR.getPublic())));

        assertThatThrownBy(() -> fetcherAllowingEverything().fetch(keyId))
                .isInstanceOf(RemoteKeyFetchException.class)
                .hasMessageContaining("Unexpected status 302");
        assertThat(requestCounts.get("/internal")).hasValue(0);
    }

    @Test
    void doesNotRequestWhenPolicyRejects() {
        URI actor = url("/users/alice");
        URI keyId = URI.create(actor + "#main-key");
        serve("/users/alice", 200, Map.of("Content-Type", "application/activity+json"),
                actorJson(keyId, actor, TestKeys.toPem(KEY_PAIR.getPublic())));
        RemoteActorKeyFetcher fetcher = new RemoteActorKeyFetcher(
                new FederationClientConfiguration().federationRestClient(),
                uri -> {
                    throw new UnsafeRemoteUrlException("blocked by test policy");
                });

        assertThatThrownBy(() -> fetcher.fetch(keyId))
                .isInstanceOf(RemoteKeyFetchException.class)
                .hasMessageContaining("Refusing to fetch")
                .hasMessageContaining("blocked by test policy");
        assertThat(requestCounts.get("/users/alice")).hasValue(0);
    }

    @Test
    void rejectsMismatchedKeyId() {
        URI actor = url("/users/alice");
        URI keyId = URI.create(actor + "#main-key");
        URI otherKeyId = URI.create(actor + "#other-key");
        serve("/users/alice", 200, Map.of("Content-Type", "application/activity+json"),
                actorJson(otherKeyId, actor, TestKeys.toPem(KEY_PAIR.getPublic())));

        assertThatThrownBy(() -> fetcherAllowingEverything().fetch(keyId))
                .isInstanceOf(RemoteKeyFetchException.class)
                .hasMessageContaining("publicKey.id does not match");
    }

    @Test
    void rejectsNonSuccessStatus() {
        URI actor = url("/users/alice");
        URI keyId = URI.create(actor + "#main-key");
        serve("/users/alice", 404, Map.of(), "");

        assertThatThrownBy(() -> fetcherAllowingEverything().fetch(keyId))
                .isInstanceOf(RemoteKeyFetchException.class)
                .hasMessageContaining("Unexpected status 404");
    }

    @Test
    void rejectsDocumentWithoutPublicKey() {
        URI actor = url("/users/alice");
        URI keyId = URI.create(actor + "#main-key");
        serve("/users/alice", 200, Map.of("Content-Type", "application/activity+json"),
                """
                        {"id": "%s", "type": "Person"}
                        """.formatted(actor));

        assertThatThrownBy(() -> fetcherAllowingEverything().fetch(keyId))
                .isInstanceOf(RemoteKeyFetchException.class)
                .hasMessageContaining("No publicKey");
    }

    @Test
    void rejectsInvalidPublicKey() {
        URI actor = url("/users/alice");
        URI keyId = URI.create(actor + "#main-key");
        serve("/users/alice", 200, Map.of("Content-Type", "application/activity+json"),
                actorJson(keyId, actor, "not a key"));

        assertThatThrownBy(() -> fetcherAllowingEverything().fetch(keyId))
                .isInstanceOf(RemoteKeyFetchException.class)
                .hasMessageContaining("Invalid public key");
    }

    private void serve(String path, int status, Map<String, String> headers, String body) {
        requestCounts.put(path, new AtomicInteger());
        server.createContext(path, exchange -> {
            requestCounts.get(path).incrementAndGet();
            lastAccept = exchange.getRequestHeaders().getFirst("Accept");
            headers.forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static String actorJson(URI keyId, URI owner, String publicKeyPem) {
        return """
                {
                  "id": "%s",
                  "type": "Person",
                  "publicKey": {
                    "id": "%s",
                    "owner": "%s",
                    "publicKeyPem": "%s"
                  }
                }
                """.formatted(owner, keyId, owner, publicKeyPem.replace("\n", "\\n"));
    }

    private static RemoteActorKeyFetcher fetcherAllowingEverything() {
        return new RemoteActorKeyFetcher(new FederationClientConfiguration().federationRestClient(), uri -> {});
    }
}
