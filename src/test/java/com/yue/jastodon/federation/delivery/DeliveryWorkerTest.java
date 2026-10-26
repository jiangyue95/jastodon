package com.yue.jastodon.federation.delivery;

import com.yue.jastodon.federation.remote.ActivityPubClient;
import com.yue.jastodon.federation.remote.RemoteFetchException;
import com.yue.jastodon.federation.remote.UnsafeRemoteUrlException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryWorkerTest {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");
    private static final URI INBOX = URI.create("https://remote.test/users/alice/inbox");
    private static final String BODY = "{\"type\":\"Accept\"}";
    private static final byte[] BODY_BYTES = BODY.getBytes(StandardCharsets.UTF_8);

    private final DeliveryRepository repository = mock(DeliveryRepository.class);
    private final ActivityPubClient client = mock(ActivityPubClient.class);
    private final DeliveryWorker worker = new DeliveryWorker(
            Clock.fixed(NOW, ZoneOffset.UTC),
            repository,
            client,
            new DeliveryRetryPolicy());

    @Test
    void markAcceptedDeliveryAsDelivered() {
        when(repository.findDue(NOW)).thenReturn(List.of(pending(42, 0)));
        when(client.post(eq(INBOX), aryEq(BODY_BYTES))).thenReturn(HttpStatusCode.valueOf(202));

        worker.deliverDue();

        verify(repository).markDelivered(42);
    }

    @Test
    void schedulesRetryForTemporaryFailure() {
        when(repository.findDue(NOW)).thenReturn(List.of(pending(42, 2)));
        when(client.post(eq(INBOX), aryEq(BODY_BYTES))).thenReturn(HttpStatusCode.valueOf(503));

        worker.deliverDue();

        verify(repository).scheduleRetry(
                42,
                "Unexpected status 503 SERVICE_UNAVAILABLE from https://remote.test/users/alice/inbox",
                Instant.parse("2026-10-07T10:04:00Z"));
    }

    @Test
    void givesUpWhenRetriesAreExhausted() {
        when(repository.findDue(NOW)).thenReturn(List.of(pending(42, 12)));
        when(client.post(eq(INBOX), aryEq(BODY_BYTES))).thenReturn(HttpStatusCode.valueOf(503));

        worker.deliverDue();

        verify(repository).markFailed(
                42,
                "Unexpected status 503 SERVICE_UNAVAILABLE from https://remote.test/users/alice/inbox");
    }

    @Test
    void givesUpOnPermanentFailure() {
        when(repository.findDue(NOW)).thenReturn(List.of(pending(42, 0)));
        when(client.post(eq(INBOX), aryEq(BODY_BYTES))).thenReturn(HttpStatusCode.valueOf(404));

        worker.deliverDue();

        verify(repository).markFailed(
                42,
                "Unexpected status 404 NOT_FOUND from https://remote.test/users/alice/inbox");
    }

    @Test
    void schedulesRetryWhenInboxCannotBeReached() {
        when(repository.findDue(NOW)).thenReturn(List.of(pending(42, 2)));
        when(client.post(eq(INBOX), aryEq(BODY_BYTES)))
                .thenThrow(new RemoteFetchException("Cannot deliver to https://remote.test/users/alice/inbox"));

        worker.deliverDue();

        verify(repository).scheduleRetry(
                42,
                "Cannot deliver to https://remote.test/users/alice/inbox",
                Instant.parse("2026-10-07T10:04:00Z"));
    }

    @Test
    void givesUpOnUnsafeInbox() {
        when(repository.findDue(NOW)).thenReturn(List.of(pending(42, 0)));
        when(client.post(eq(INBOX), aryEq(BODY_BYTES)))
                .thenThrow(new UnsafeRemoteUrlException(
                        "URL resolves to a non-public address: https://remote.test/users/alice/inbox"));

        worker.deliverDue();

        verify(repository).markFailed(
                42,
                "URL resolves to a non-public address: https://remote.test/users/alice/inbox");
    }

    private static Delivery pending(long id, int attempts) {
        return new Delivery(id, INBOX, BODY, DeliveryStatus.PENDING, attempts, NOW, null, NOW);
    }
}
