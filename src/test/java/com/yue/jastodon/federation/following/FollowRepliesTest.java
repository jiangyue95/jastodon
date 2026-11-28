package com.yue.jastodon.federation.following;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FollowRepliesTest extends AbstractIntegrationTest {

    private static final URI FOLLOW_ID =
            URI.create("https://jastodon.test/users/yue#follows/2ba78572-9429-4cd3-ab4a-bd82f9a5659b");
    private static final URI OTHER_FOLLOW_ID =
            URI.create("https://jastodon.test/users/yue#follows/75362f45-fe45-473f-9114-abfc7cc43513");
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-07T11:00:00.123456789Z"), ZoneOffset.UTC);
    private static final Instant FETCHED = Instant.parse("2026-10-07T09:00:00Z");
    private static final Instant REQUESTED = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant FIRST_ACCEPTED = Instant.parse("2026-10-07T10:30:00Z");
    private static final Instant NOW = Instant.parse("2026-10-07T11:00:00.123456Z");
    private static final URI ALICE = URI.create("https://mastodon.example/users/alice");
    private static final URI BOB = URI.create("https://mastodon.example/users/bob");
    private static final URI ALICE_INBOX = URI.create("https://mastodon.example/users/alice/inbox");

    @Autowired
    RemoteAccountRepository remoteAccountRepository;

    @Autowired
    FollowRepository followRepository;

    @Autowired
    JdbcClient jdbcClient;

    long aliceId;
    long followRowId;

    FollowReplies followReplies;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM follows").update();
        jdbcClient.sql("DELETE FROM remote_accounts").update();
        aliceId = remoteAccountRepository.save(ALICE, "alice@mastodon.example", ALICE_INBOX, null, null, FETCHED);
        followRowId = followRepository.saveRequest(aliceId, FOLLOW_ID, REQUESTED).orElseThrow();
        followReplies = new FollowReplies(followRepository, CLOCK);
    }

    @Test
    void acceptsAPendingFollow() {
        assertThat(followReplies.accept(ALICE, FOLLOW_ID)).isTrue();

        assertThat(followRepository.findByRemoteAccountId(aliceId)).contains(new Follow(
                followRowId,
                aliceId,
                FOLLOW_ID,
                FollowState.ACCEPTED,
                REQUESTED,
                NOW));
    }

    @Test
    void keepsTheFirstAcceptanceTime() {
        markAccepted(FIRST_ACCEPTED);

        assertThat(followReplies.accept(ALICE, FOLLOW_ID)).isTrue();

        assertThat(followRepository.findByRemoteAccountId(aliceId)).contains(new Follow(
                followRowId,
                aliceId,
                FOLLOW_ID,
                FollowState.ACCEPTED,
                REQUESTED,
                FIRST_ACCEPTED));
    }

    @Test
    void acceptsNothingForAnotherFollowId() {
        assertThat(followReplies.accept(ALICE, OTHER_FOLLOW_ID)).isFalse();

        assertThat(followRepository.findByRemoteAccountId(aliceId)).contains(new Follow(
                followRowId,
                aliceId,
                FOLLOW_ID,
                FollowState.PENDING,
                REQUESTED,
                null));
    }

    @Test
    void acceptsNothingFromAnotherActor() {
        assertThat(followReplies.accept(BOB, FOLLOW_ID)).isFalse();

        assertThat(followRepository.findByRemoteAccountId(aliceId)).contains(new Follow(
                followRowId,
                aliceId,
                FOLLOW_ID,
                FollowState.PENDING,
                REQUESTED,
                null));
    }

    @Test
    void rejectsAPendingFollow() {
        assertThat(followReplies.reject(ALICE, FOLLOW_ID)).isTrue();

        assertThat(followRepository.findByRemoteAccountId(aliceId)).isEmpty();
    }

    @Test
    void rejectsAnAcceptedFollow() {
        markAccepted(FIRST_ACCEPTED);

        assertThat(followReplies.reject(ALICE, FOLLOW_ID)).isTrue();

        assertThat(followRepository.findByRemoteAccountId(aliceId)).isEmpty();
    }

    @Test
    void rejectsNothingForAnotherFollowId() {
        assertThat(followReplies.reject(ALICE, OTHER_FOLLOW_ID)).isFalse();

        assertThat(followRepository.findByRemoteAccountId(aliceId)).contains(new Follow(
                followRowId,
                aliceId,
                FOLLOW_ID,
                FollowState.PENDING,
                REQUESTED,
                null));
    }

    @Test
    void rejectsNothingFromAnotherActor() {
        assertThat(followReplies.reject(BOB, FOLLOW_ID)).isFalse();

        assertThat(followRepository.findByRemoteAccountId(aliceId)).contains(new Follow(
                followRowId,
                aliceId,
                FOLLOW_ID,
                FollowState.PENDING,
                REQUESTED,
                null));
    }

    private void markAccepted(Instant acceptedAt) {
        jdbcClient.sql("""
                       UPDATE follows
                       SET state = :state, accepted_at = :acceptedAt
                       WHERE id = :id
                       """)
                .param("state", FollowState.ACCEPTED.name())
                .param("acceptedAt", acceptedAt.atOffset(ZoneOffset.UTC))
                .param("id", followRowId)
                .update();
    }
}
