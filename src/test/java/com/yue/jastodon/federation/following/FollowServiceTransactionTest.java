package com.yue.jastodon.federation.following;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class FollowServiceTransactionTest extends AbstractIntegrationTest {

    private static final URI ALICE = URI.create("https://mastodon.example/users/alice");
    private static final URI ALICE_INBOX = URI.create("https://mastodon.example/users/alice/inbox");
    private static final Instant FETCHED = Instant.parse("2026-10-07T09:00:00Z");

    @Autowired
    FollowService followService;

    @Autowired
    RemoteAccountRepository remoteAccountRepository;

    @Autowired
    JdbcClient jdbcClient;

    @MockitoSpyBean
    FollowRepository followRepository;

    @MockitoSpyBean
    DeliveryRepository deliveryRepository;

    long aliceId;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM follows").update();
        jdbcClient.sql("DELETE FROM deliveries").update();
        jdbcClient.sql("DELETE FROM remote_accounts").update();
        aliceId = remoteAccountRepository.save(
                ALICE, "alice@mastodon.example", ALICE_INBOX, null, null, FETCHED);
    }

    @Test
    void savesNoFollowWhenTheFollowCannotBeEnqueued() {
        doThrow(new DataAccessResourceFailureException("database down"))
                .when(deliveryRepository).enqueue(any(), any(), any());

        assertThatThrownBy(() -> followService.follow(aliceId))
                .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(followRepository.findByRemoteAccountId(aliceId)).isEmpty();
    }

    @Test
    void enqueuesNothingWhenTheFollowCannotBeSaved() {
        doThrow(new DataAccessResourceFailureException("database down"))
                .when(followRepository).saveRequest(anyLong(), any(), any());

        assertThatThrownBy(() -> followService.follow(aliceId))
                .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM deliveries").query(Long.class).single()).isZero();
    }
}
