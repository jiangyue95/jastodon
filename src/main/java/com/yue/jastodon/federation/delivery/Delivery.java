package com.yue.jastodon.federation.delivery;

import java.net.URI;
import java.time.Instant;

record Delivery(
        long id,
        URI inbox,
        String body,
        DeliveryStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt) {
}
