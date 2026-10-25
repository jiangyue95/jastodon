package com.yue.jastodon.federation.delivery;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

class DeliveryRetryPolicy {

    private static final List<Duration> DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(2),
            Duration.ofMinutes(4),
            Duration.ofMinutes(8),
            Duration.ofMinutes(16),
            Duration.ofMinutes(32),
            Duration.ofHours(1),
            Duration.ofHours(2),
            Duration.ofHours(4),
            Duration.ofHours(8),
            Duration.ofHours(16),
            Duration.ofHours(24));

    Optional<Instant> nextAttemptAt(int failedAttempts, Instant now) {
        if (failedAttempts > DELAYS.size()) {
            return Optional.empty();
        }
        return Optional.of(now.plus(DELAYS.get(failedAttempts - 1)));
    }
}
