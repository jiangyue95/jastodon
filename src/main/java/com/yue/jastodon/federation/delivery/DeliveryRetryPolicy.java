package com.yue.jastodon.federation.delivery;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
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
    private static final Set<Integer> RETRYABLE_CLIENT_ERRORS = Set.of(401, 408, 429);

    Optional<Instant> nextAttemptAt(int failedAttempts, Instant now) {
        if (failedAttempts > DELAYS.size()) {
            return Optional.empty();
        }
        return Optional.of(now.plus(DELAYS.get(failedAttempts - 1)));
    }

    boolean isRetryable(HttpStatusCode status) {
        return status.is5xxServerError() || RETRYABLE_CLIENT_ERRORS.contains(status.value());
    }
}
