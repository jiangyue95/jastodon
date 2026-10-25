package com.yue.jastodon.federation.remote;

import com.sun.net.httpserver.HttpServer;
import com.yue.jastodon.federation.signature.HttpSignatureSigner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RemoteActorFetcherTest {

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
    void fetchesTheActorsInbox() {
        URI actor = url("/users/alice");
        URI inbox = url("/users/alice/inbox");
        serve("/users/alice", """
                {"id": "%s", "type": "Person", "inbox": "%s"}
                """.formatted(actor, inbox));

        assertThat(fetcher().fetch(actor)).isEqualTo(new RemoteActor(actor, inbox));
    }

    @Test
    void rejectsDocumentForAnotherActor() {
        URI alice = url("/users/alice");
        URI bob = url("/users/bob");
        URI inbox = url("/users/alice/inbox");
        serve("/users/alice", """
                {"id": "%s", "type": "Person", "inbox": "%s"}
                """.formatted(bob, inbox));

        assertThatThrownBy(() -> fetcher().fetch(alice))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsActorWithoutInbox() {
        URI alice = url("/users/alice");
        serve("/users/alice", """
                {"id": "%s", "type": "Person"}
                """.formatted(alice));

        assertThatThrownBy(() -> fetcher().fetch(alice))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessageContaining("No inbox");
    }

    private void serve(String path, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/activity+json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static RemoteActorFetcher fetcher() {
        return new RemoteActorFetcher(new ActivityPubClient(
                new FederationClientConfiguration().federationRestClient(),
                uri -> {},
                mock(HttpSignatureSigner.class)));
    }
}
