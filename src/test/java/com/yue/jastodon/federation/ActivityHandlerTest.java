package com.yue.jastodon.federation;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.follower.Follower;
import com.yue.jastodon.federation.follower.FollowerRepository;
import com.yue.jastodon.federation.remote.RemoteActor;
import com.yue.jastodon.federation.remote.RemoteActorFetcher;
import com.yue.jastodon.federation.remote.RemoteFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
class ActivityHandlerTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");
    private static final URI ALICE = URI.create("https://remote.test/users/alice");
    private static final URI ALICE_INBOX = URI.create("https://remote.test/users/alice/inbox");
    private static final URI FOLLOW_ID = URI.create("https://remote.test/activities/follow-1");
    private static final URI YUE = URI.create("https://jastodon.test/users/yue");

    @Autowired
    ActivityHandler activityHandler;

    @Autowired
    FollowerRepository followerRepository;

    @Autowired
    JdbcClient jdbcClient;

    @MockitoBean
    RemoteActorFetcher remoteActorFetcher;

    @MockitoBean
    Clock clock;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM followers").update();
        when(remoteActorFetcher.fetch(ALICE)).thenReturn(new RemoteActor(ALICE, ALICE_INBOX));
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void followOfOurActorStoresTheFollower() {
        activityHandler.handle(new IncomingActivity(FOLLOW_ID, "Follow", ALICE, YUE));

        assertThat(followerRepository.findAll()).containsExactly(new Follower(ALICE, ALICE_INBOX, FOLLOW_ID, NOW));
    }

    @Test
    void ignoresOtherActivityTypes() {
        activityHandler.handle(new IncomingActivity(FOLLOW_ID, "Like", ALICE, YUE));

        assertThat(followerRepository.findAll()).isEmpty();
        verifyNoInteractions(remoteActorFetcher);
    }

    @Test
    void ignoresFollowOfSomeoneElse() {
        activityHandler.handle(new IncomingActivity(
                FOLLOW_ID, "Follow", ALICE, URI.create("https://jastodon.test/users/bob")));

        assertThat(followerRepository.findAll()).isEmpty();
        verifyNoInteractions(remoteActorFetcher);
    }

    @Test
    void followIsRetriedLaterWhenTheActorCannotBeFetched() {
        when(remoteActorFetcher.fetch(ALICE)).thenThrow(new RemoteFetchException("unreachable"));

        assertThatThrownBy(() -> activityHandler.handle(new IncomingActivity(FOLLOW_ID, "Follow", ALICE, YUE)))
                .isInstanceOf(RetryLaterException.class);
        assertThat(followerRepository.findAll()).isEmpty();
    }

    @Test
    void rejectsFollowWithoutId() {
        assertThatThrownBy(() -> activityHandler.handle(new IncomingActivity(null, "Follow", ALICE, YUE)))
                .isInstanceOf(InvalidActivityException.class)
                .hasMessageContaining("no id");
        assertThat(followerRepository.findAll()).isEmpty();
        verifyNoInteractions(remoteActorFetcher);
    }
 }
