package com.yue.jastodon.federation.following;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FollowRepositoryTest extends AbstractIntegrationTest {

    private static final URI ALICE = URI.create("https://mastodon.example/users/alice");
    private static final URI ALICE_INBOX = URI.create("https://mastodon.example/users/alice/inbox");
    private static final URI BOB = URI.create("https://mastodon.example/users/bob");
    private static final URI BOB_INBOX = URI.create("https://mastodon.example/users/bob/inbox");
    private static final URI FOLLOW_ID = URI.create("https://jastodon.test/activities/follow-1");
    private static final Instant FETCHED = Instant.parse("2026-10-07T09:00:00Z");
    private static final Instant REQUESTED = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant ACCEPTED = Instant.parse("2026-10-07T10:05:00Z");

    @Autowired
    FollowRepository followRepository;

    @Autowired
    RemoteAccountRepository remoteAccountRepository;

    @Autowired
    JdbcClient jdbcClient;

    long aliceId;
    long bobId;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM follows").update();
        jdbcClient.sql("DELETE FROM remote_accounts").update();
        aliceId = remoteAccountRepository.save(
                ALICE, "alice@mastodon.example", ALICE_INBOX, null, null, FETCHED);
        bobId = remoteAccountRepository.save(
                BOB, "bob@mastodon.example", BOB_INBOX, null, null, FETCHED);
    }

    @Test
    void savesAndFindsAPendingRequest() {
        long id = followRepository.saveRequest(aliceId, FOLLOW_ID, REQUESTED);

        assertThat(followRepository.findByRemoteAccountId(aliceId)).contains(new Follow(
                id,
                aliceId,
                FOLLOW_ID,
                FollowState.PENDING,
                REQUESTED,
                null));
    }

    @Test
    void findsNothingForAnAccountThatIsNotFollowed() {
        followRepository.saveRequest(aliceId, FOLLOW_ID, REQUESTED);

        assertThat(followRepository.findByRemoteAccountId(bobId)).isEmpty();
    }

    @Test
    void rejectsASecondRequestForTheSameAccount() {
        followRepository.saveRequest(aliceId, FOLLOW_ID, REQUESTED);

        assertThatThrownBy(() -> followRepository.saveRequest(
                aliceId, URI.create("https://jastodon.test/activities/follow-2"), REQUESTED))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("follows_remote_account_id_key");
    }

    @Test
    void rejectsADuplicateFollowActivityId() {
        followRepository.saveRequest(aliceId, FOLLOW_ID, REQUESTED);

        assertThatThrownBy(() -> followRepository.saveRequest(
                bobId, FOLLOW_ID, REQUESTED))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("follows_follow_activity_id_key");
    }

    @Test
    void rejectsARequestForAnUnknownAccount() {
        assertThatThrownBy(() -> followRepository.saveRequest(
                bobId + 1000, URI.create("https://jastodon.test/activities/follow-2"), REQUESTED))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("follows_remote_account_id_fkey");
    }

    @Test
    void keepsAnAccountThatIsFollowed() {
        followRepository.saveRequest(aliceId, FOLLOW_ID, REQUESTED);

        assertThatThrownBy(() -> jdbcClient.sql("DELETE FROM remote_accounts WHERE id = :id")
                    .param("id", aliceId)
                    .update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("follows_remote_account_id_fkey");
        assertThat(remoteAccountRepository.findByActorId(ALICE)).isPresent();
    }

    @Test
    void readsAnAcceptedFollow() {
        long followRowId = jdbcClient.sql("""
                        INSERT INTO follows (remote_account_id, follow_activity_id, state, requested_at, accepted_at)
                        VALUES (:remoteAccountId, :followActivityId, :state, :requestedAt, :acceptedAt)
                        RETURNING id
                        """)
                .param("remoteAccountId", aliceId)
                .param("followActivityId", FOLLOW_ID.toString())
                .param("state", FollowState.ACCEPTED.name())
                .param("requestedAt", REQUESTED.atOffset(ZoneOffset.UTC))
                .param("acceptedAt", ACCEPTED.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();

        assertThat(followRepository.findByRemoteAccountId(aliceId))
                .contains(new Follow(
                        followRowId,
                        aliceId,
                        FOLLOW_ID,
                        FollowState.ACCEPTED,
                        REQUESTED,
                        ACCEPTED));
    }
}
