package com.yue.jastodon.federation.following;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FollowServiceTest extends AbstractIntegrationTest {

    private static final UUID FOLLOW_UUID = UUID.fromString("2ba78572-9429-4cd3-ab4a-bd82f9a5659b");
    private static final URI FOLLOW_ID = URI.create("https://jastodon.test/users/yue#follows/2ba78572-9429-4cd3-ab4a-bd82f9a5659b");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T10:00:00.123456789Z"), ZoneOffset.UTC);
    private static final Instant FETCHED = Instant.parse("2026-10-07T09:00:00Z");
    private static final Instant REQUESTED = Instant.parse("2026-10-07T10:00:00.123456Z");
    private static final URI ALICE = URI.create("https://mastodon.example/users/alice");
    private static final URI ALICE_INBOX = URI.create("https://mastodon.example/users/alice/inbox");
    private static final URI EXISTING_FOLLOW_ID = URI.create("https://jastodon.test/users/yue#follows/78a5baa9-95eb-481e-b9c9-f5d1c92d543b");
    private static final Instant EXISTING_REQUESTED = Instant.parse("2026-10-06T10:00:00Z");
    private static final Instant EXISTING_ACCEPTED = Instant.parse("2026-10-06T10:05:00Z");

    @Autowired
    RemoteAccountRepository remoteAccountRepository;

    @Autowired
    FollowRepository followRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    FederationProperties federationProperties;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    JdbcClient jdbcClient;

    long aliceId;

    FollowService followService;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM follows").update();
        jdbcClient.sql("DELETE FROM deliveries").update();
        jdbcClient.sql("DELETE FROM remote_accounts").update();
        aliceId = remoteAccountRepository.save(
                ALICE, "alice@mastodon.example", ALICE_INBOX, null, null, FETCHED);
        followService = new FollowService(
                remoteAccountRepository,
                followRepository,
                deliveryRepository,
                federationProperties,
                jsonMapper,
                () -> FOLLOW_UUID,
                CLOCK);
    }

    @Test
    void followsAnAccount() {
        FollowResult result = followService.follow(aliceId).orElseThrow();

        Follow stored = followRepository.findByRemoteAccountId(aliceId).orElseThrow();
        assertThat(stored).isEqualTo(new Follow(
                stored.id(),
                aliceId,
                FOLLOW_ID,
                FollowState.PENDING,
                REQUESTED,
                null));
        assertThat(result).isEqualTo(new FollowResult(stored, true));
    }

    @Test
    void enqueuesTheFollowToTheAccountsInbox() {
        followService.follow(aliceId);

        Map<String, Object> delivery = jdbcClient.sql("SELECT inbox, body FROM deliveries")
                .query()
                .singleRow();
        assertThat(delivery).containsEntry("inbox", ALICE_INBOX.toString());
        assertThat(jsonMapper.readTree((String)delivery.get("body"))).isEqualTo(jsonMapper.readTree("""
                {
                  "@context": "https://www.w3.org/ns/activitystreams",
                  "id": "https://jastodon.test/users/yue#follows/2ba78572-9429-4cd3-ab4a-bd82f9a5659b",
                  "type": "Follow",
                  "actor": "https://jastodon.test/users/yue",
                  "object": "https://mastodon.example/users/alice"
                }
                """));
        OffsetDateTime nextAttemptAt = jdbcClient.sql("SELECT next_attempt_at FROM deliveries")
                .query((rs, rowNum) -> rs.getObject("next_attempt_at", OffsetDateTime.class))
                .single();
        assertThat(nextAttemptAt.toInstant()).isEqualTo(REQUESTED);
    }

    @Test
    void keepsAnExistingFollow() {
        long followRowId = jdbcClient.sql("""
                                          INSERT INTO follows (
                                                               remote_account_id,
                                                               follow_activity_id,
                                                               state, requested_at,
                                                               accepted_at)
                                          VALUES (
                                                  :remoteAccountId,
                                                  :followActivityId, 
                                                  :state, 
                                                  :requestedAt,
                                                  :acceptedAt)
                                          RETURNING id
                                          """)
                .param("remoteAccountId", aliceId)
                .param("followActivityId", EXISTING_FOLLOW_ID.toString())
                .param("state", FollowState.ACCEPTED.name())
                .param("requestedAt", EXISTING_REQUESTED.atOffset(ZoneOffset.UTC))
                .param("acceptedAt", EXISTING_ACCEPTED.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
        Follow existing = new Follow(
                followRowId,
                aliceId,
                EXISTING_FOLLOW_ID,
                FollowState.ACCEPTED,
                EXISTING_REQUESTED,
                EXISTING_ACCEPTED);

        assertThat(followService.follow(aliceId)).contains(new FollowResult(existing, false));
        assertThat(followRepository.findByRemoteAccountId(aliceId)).contains(existing);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM deliveries").query(Long.class).single()).isZero();
    }

    @Test
    void followsNothingForAnUnknownAccount() {
        assertThat(followService.follow(aliceId + 1000)).isEmpty();
    }
}
