package com.yue.jastodon.federation.following;

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
class RemoteAccountRepositoryTest extends AbstractIntegrationTest {

    private static final URI ALICE = URI.create("https://mastodon.example/users/alice");
    private static final URI ALICE_INBOX = URI.create("https://mastodon.example/users/alice/inbox");
    private static final URI ALICE_AVATAR = URI.create("https://mastodon.example/avatars/alice.png");
    private static final URI BOB = URI.create("https://mastodon.example/users/bob");
    private static final Instant FETCHED = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant REFETCHED = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    RemoteAccountRepository remoteAccountRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void emptyTable() {
        jdbcClient.sql("DELETE FROM remote_accounts").update();
    }

    @Test
    void savesAndFindsAnAccount() {
        long id = remoteAccountRepository.save(
                ALICE,
                "alice@mastodon.example",
                ALICE_INBOX,
                "Alice",
                ALICE_AVATAR,
                FETCHED);

        assertThat(remoteAccountRepository.findByActorId(ALICE)).contains(new RemoteAccount(
                id,
                ALICE,
                "alice@mastodon.example",
                ALICE_INBOX,
                "Alice",
                ALICE_AVATAR,
                FETCHED));
    }

    @Test
    void savingTheSameActorAgainUpdatesItAndKeepsItsId() {
        URI newInbox = URI.create("https://mastodon.example/inbox");
        URI newAvatar = URI.create("https://files.mastodon.example/avatars/alice-2.png");

        long firstId = remoteAccountRepository.save(
                ALICE,
                "alice@mastodon.example",
                ALICE_INBOX,
                "Alice",
                ALICE_AVATAR,
                FETCHED);
        long secondId = remoteAccountRepository.save(
                ALICE,
                "alice@example.com",
                newInbox,
                "Alice B.",
                newAvatar,
                REFETCHED);

        assertThat(secondId).isEqualTo(firstId);
        assertThat(remoteAccountRepository.findByActorId(ALICE)).contains(new RemoteAccount(
                firstId,
                ALICE,
                "alice@example.com",
                newInbox,
                "Alice B.",
                newAvatar,
                REFETCHED));
    }

    @Test
    void keepsAnAccountWithoutNameOrAvatar() {
        long id = remoteAccountRepository.save(
                ALICE,
                "alice@mastodon.example",
                ALICE_INBOX,
                null,
                null,
                FETCHED);

        assertThat(remoteAccountRepository.findByActorId(ALICE)).contains(new RemoteAccount(
                id,
                ALICE,
                "alice@mastodon.example",
                ALICE_INBOX,
                null,
                null,
                FETCHED));
    }

    @Test
    void findsNothingForAnUnknownActor() {
        remoteAccountRepository.save(
                BOB,
                "bob@mastodon.example",
                ALICE_INBOX,
                null,
                null,
                FETCHED);

        assertThat(remoteAccountRepository.findByActorId(ALICE)).isEmpty();
    }
}
