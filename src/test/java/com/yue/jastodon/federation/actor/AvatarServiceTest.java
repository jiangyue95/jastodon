package com.yue.jastodon.federation.actor;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.delivery.DeliveryRepository;
import com.yue.jastodon.federation.follower.Follower;
import com.yue.jastodon.federation.follower.FollowerRepository;
import com.yue.jastodon.federation.profile.Avatars;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.util.IdGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class AvatarServiceTest extends AbstractIntegrationTest {

    private static final byte[] IMAGE = {1, 2, 3};
    private static final UUID UPDATE_ID = UUID.fromString("3f2b8c1d-6a4e-4f9b-8d2c-7e5a1b9c0d34");
    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");

    @Autowired
    AvatarService avatarService;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    FollowerRepository followerRepository;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    ActorKeys actorKeys;

    @MockitoSpyBean
    Avatars avatars;

    @MockitoSpyBean
    DeliveryRepository deliveryRepository;

    @MockitoBean
    IdGenerator idGenerator;

    @MockitoBean
    Clock clock;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM profiles").update();
        jdbcClient.sql("DELETE FROM followers").update();
        jdbcClient.sql("DELETE FROM deliveries").update();
        doReturn("avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png").when(avatars).upload(any());
        when(idGenerator.generateId()).thenReturn(UPDATE_ID);
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void changeMakesTheUploadedAvatarCurrent() {
        URI url = avatarService.change(IMAGE);

        assertThat(url).isEqualTo(
                URI.create("https://media.jastodon.test/avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png"));
        assertThat(jdbcClient.sql("SELECT avatar_key FROM profiles WHERE username = 'yue'")
                .query(String.class)
                .single())
                .isEqualTo("avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png");
        verify(avatars).upload(IMAGE);
    }

    @Test
    void changeEnqueuesAnUpdateForEveryFollower() {
        jdbcClient.sql("""
                       INSERT INTO profiles (username, avatar_key)
                       VALUES ('yue', 'avatars/1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.png')
                       """).update();
        followerRepository.save(follower("alice"));
        followerRepository.save(follower("bob"));

        avatarService.change(IMAGE);

        assertThat(jdbcClient.sql("SELECT inbox FROM deliveries ORDER BY id").query(String.class).list())
                .containsExactly(
                        "https://remote.test/users/alice/inbox",
                        "https://remote.test/users/bob/inbox");
        assertThat(jdbcClient.sql("SELECT body FROM deliveries").query(String.class).list())
                .allSatisfy(body ->
                        assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                        {
                          "@context": ["https://www.w3.org/ns/activitystreams", "https://w3id.org/security/v1"],
                          "id": "https://jastodon.test/users/yue#updates/3f2b8c1d-6a4e-4f9b-8d2c-7e5a1b9c0d34",
                          "type": "Update",
                          "actor": "https://jastodon.test/users/yue",
                          "to": ["https://www.w3.org/ns/activitystreams#Public"],
                          "object": {
                            "id": "https://jastodon.test/users/yue",
                            "type": "Person",
                            "preferredUsername": "yue",
                            "inbox": "https://jastodon.test/users/yue/inbox",
                            "outbox": "https://jastodon.test/users/yue/outbox",
                            "publicKey": {
                              "id": "https://jastodon.test/users/yue#main-key",
                              "owner": "https://jastodon.test/users/yue",
                              "publicKeyPem": %s
                            },
                            "icon": {
                              "type": "Image",
                              "mediaType": "image/png",
                              "url": "https://media.jastodon.test/avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png"
                            }
                          }
                        }
                        """.formatted(jsonMapper.writeValueAsString(actorKeys.publicKeyPem())))));
    }

    @Test
    void enqueuedUpdateIsDueImmediately() {
        followerRepository.save(follower("alice"));

        avatarService.change(IMAGE);

        OffsetDateTime nextAttemptAt = jdbcClient.sql("SELECT next_attempt_at FROM deliveries")
                .query((rs, rowNum) -> rs.getObject("next_attempt_at", OffsetDateTime.class))
                .single();

        assertThat(nextAttemptAt.toInstant()).isEqualTo(Instant.parse("2026-10-07T10:00:00Z"));
    }

    @Test
    void keepsTheCurrentAvatarWhenAnUpdateCannotBeEnqueued() {
        jdbcClient.sql("""
                       INSERT INTO profiles (username, avatar_key)
                       VALUES ('yue', 'avatars/1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.png')
                       """).update();
        followerRepository.save(follower("alice"));
        followerRepository.save(follower("bob"));
        doCallRealMethod()
                .doThrow(new DataAccessResourceFailureException("database down"))
                .when(deliveryRepository).enqueue(any(), any(), any());

        assertThatThrownBy(() -> avatarService.change(IMAGE))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("database down");
        assertThat(jdbcClient.sql("SELECT avatar_key FROM profiles WHERE username = 'yue'")
                .query(String.class)
                .single())
                .isEqualTo("avatars/1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.png");
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM deliveries").query(Long.class).single()).isZero();
    }

    @Test
    void keepsTheCurrentAvatarWhenTheUploadFails() {
        jdbcClient.sql("""
                       INSERT INTO profiles (username, avatar_key)
                       VALUES ('yue', 'avatars/1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.png')
                       """).update();
        followerRepository.save(follower("alice"));
        RuntimeException failure = new IllegalStateException("upload failed");
        doThrow(failure).when(avatars).upload(any());

        assertThatThrownBy(() -> avatarService.change(IMAGE)).isSameAs(failure);

        assertThat(jdbcClient.sql("SELECT avatar_key FROM profiles WHERE username ='yue'")
                .query(String.class)
                .single())
                .isEqualTo("avatars/1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.png");
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
