package com.yue.jastodon.federation.delivery;

import com.yue.jastodon.federation.remote.ActivityPubClient;
import com.yue.jastodon.federation.remote.RemoteFetchException;
import com.yue.jastodon.federation.remote.UnsafeRemoteUrlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Component
class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

    private final Clock clock;
    private final DeliveryRepository deliveryRepository;
    private final ActivityPubClient activityPubClient;
    private final DeliveryRetryPolicy deliveryRetryPolicy;

    DeliveryWorker(
            Clock clock,
            DeliveryRepository deliveryRepository,
            ActivityPubClient activityPubClient,
            DeliveryRetryPolicy deliveryRetryPolicy) {
        this.clock = clock;
        this.deliveryRepository = deliveryRepository;
        this.activityPubClient = activityPubClient;
        this.deliveryRetryPolicy = deliveryRetryPolicy;
    }

    void deliverDue() {
        Instant now = clock.instant();
        for (Delivery delivery : deliveryRepository.findDue(now)) {
            try {
                deliver(delivery, now);
            } catch (RuntimeException e) {
                log.error(
                        "Delivery {} to {} failed unexpectedly and stays pending", delivery.id(), delivery.inbox(), e);
            }

        }
    }

    private void deliver(Delivery delivery, Instant now) {
        byte[] body = delivery.body().getBytes(StandardCharsets.UTF_8);
        HttpStatusCode status;
        try {
            status = activityPubClient.post(delivery.inbox(), body);
        } catch (RemoteFetchException e) {
            retryOrGiveUp(delivery, e.getMessage(), now);
            return;
        } catch (UnsafeRemoteUrlException e) {
            deliveryRepository.markFailed(delivery.id(), e.getMessage());
            return;
        }
        if (status.is2xxSuccessful()) {
            deliveryRepository.markDelivered(delivery.id());
            return;
        }

        String error = "Unexpected status " + status + " from " + delivery.inbox();
        if (!deliveryRetryPolicy.isRetryable(status)) {
            deliveryRepository.markFailed(delivery.id(), error);
            return;
        }

        retryOrGiveUp(delivery, error, now);
    }

    private void retryOrGiveUp(Delivery delivery, String error, Instant now) {
        Optional<Instant> nextAttemptAt = deliveryRetryPolicy.nextAttemptAt(delivery.attempts() + 1, now);
        if (nextAttemptAt.isPresent()) {
            deliveryRepository.scheduleRetry(delivery.id(), error, nextAttemptAt.get());
        } else {
            deliveryRepository.markFailed(delivery.id(), error);
        }
    }
}
