package com.yue.jastodon.federation.note;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import com.yue.jastodon.federation.follower.Follower;
import com.yue.jastodon.federation.follower.FollowerRepository;
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
import static org.mockito.Mockito.doCallRealMethod;


@SpringBootTest
class NoteServiceTransactionTest extends AbstractIntegrationTest {

    @Autowired
    NoteService noteService;

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    FollowerRepository followerRepository;

    @Autowired
    JdbcClient jdbcClient;

    @MockitoSpyBean
    DeliveryRepository deliveryRepository;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM notes").update();
        jdbcClient.sql("DELETE FROM followers").update();
        jdbcClient.sql("DELETE FROM deliveries").update();
    }

    @Test
    void storesNothingWhenADeliveryCannotBeEnqueued() {
        followerRepository.save(follower("alice"));
        followerRepository.save(follower("bob"));
        doCallRealMethod()
                .doThrow(new DataAccessResourceFailureException("database down"))
                .when(deliveryRepository).enqueue(any(), any(), any());

        assertThatThrownBy(() -> noteService.create("hello"))
                .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(noteRepository.findAll()).isEmpty();
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM deliveries").query(Long.class).single()).isZero();
    }

    private static Follower follower(String name) {
        return new Follower(
                URI.create("https://remote.test/users/" + name),
                URI.create("https://remote.test/users/" + name + "/inbox"),
                URI.create("https://remote.test/follows/" + name),
                Instant.parse("2026-10-07T09:00:00Z"));
    }
}
