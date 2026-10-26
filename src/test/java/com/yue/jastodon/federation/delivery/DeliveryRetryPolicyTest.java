package com.yue.jastodon.federation.delivery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;


class DeliveryRetryPolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");

    private final DeliveryRetryPolicy policy = new DeliveryRetryPolicy();

    @Test
    void firstFailureIsRetriedAfterOneMinute() {
        assertThat(policy.nextAttemptAt(1, NOW)).contains(Instant.parse("2026-10-07T10:01:00Z"));
    }

    @Test
    void lastRetryWaitsADay() {
        assertThat(policy.nextAttemptAt(12, NOW)).contains(Instant.parse("2026-10-08T10:00:00Z"));
    }

    @Test
    void givesUpAfterTheLastRetry() {
        assertThat(policy.nextAttemptAt(13, NOW)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504, 401, 408, 429})
    void retriesTemporaryFailures(int status) {
        assertThat(policy.isRetryable(HttpStatusCode.valueOf(status))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 410, 422, 302})
    void givesUpOnPermanentFailures(int status) {
        assertThat(policy.isRetryable(HttpStatusCode.valueOf(status))).isFalse();
    }
}
