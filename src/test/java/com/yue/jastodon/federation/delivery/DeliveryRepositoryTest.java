package com.yue.jastodon.federation.delivery;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeliveryRepositoryTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-07T11:00:00Z");
    private static final String ERROR =
            "Unexpected status 503 SERVICE_UNAVAILABLE from https://remote.test/users/alice/inbox";
    private static final URI INBOX = URI.create("https://remote.test/users/alice/inbox");
    private static final String BODY = """
            {
              "type": "Accept",
              "actor": "https://jastodon.test/users/yue"
            }
            """;

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void emptyTable() {
        jdbcClient.sql("DELETE FROM deliveries").update();
    }

    @Test
    void enqueuedDeliveryIsDueImmediately() {
        long id = deliveryRepository.enqueue(INBOX, BODY, NOW);

        assertThat(deliveryRepository.findDue(NOW)).containsExactly(new Delivery(
                id,
                INBOX,
                BODY,
                DeliveryStatus.PENDING,
                0,
                NOW,
                null,
                NOW));
    }

    @Test
    void deliveredDeliveryIsNoLongerDue() {
        long id = deliveryRepository.enqueue(INBOX, BODY, NOW);
        deliveryRepository.markDelivered(id);

        assertThat(deliveryRepository.findDue(NOW)).isEmpty();
        assertThat(jdbcClient.sql("SELECT status, attempts FROM deliveries WHERE id = :id")
                .param("id", id)
                .query()
                .singleRow())
                .containsEntry("status", "DELIVERED")
                .containsEntry("attempts", 1);
    }

    @Test
    void retriedDeliveryIsDueAgainAtItsNextAttempt() {
        long id = deliveryRepository.enqueue(INBOX, BODY, NOW);
        deliveryRepository.scheduleRetry(id, ERROR, LATER);

        assertThat(deliveryRepository.findDue(NOW)).isEmpty();
        assertThat(deliveryRepository.findDue(LATER)).containsExactly(new Delivery(
                id,
                INBOX,
                BODY,
                DeliveryStatus.PENDING,
                1,
                LATER,
                ERROR,
                NOW));
    }
}
