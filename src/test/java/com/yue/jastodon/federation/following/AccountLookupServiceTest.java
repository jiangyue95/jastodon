package com.yue.jastodon.federation.following;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.remote.Acct;
import com.yue.jastodon.federation.remote.RemoteActor;
import com.yue.jastodon.federation.remote.RemoteActorFetcher;
import com.yue.jastodon.federation.remote.RemoteFetchException;
import com.yue.jastodon.federation.remote.WebFingerAccount;
import com.yue.jastodon.federation.remote.WebFingerClient;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
class AccountLookupServiceTest extends AbstractIntegrationTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-07T10:00:00.123456789Z"), ZoneOffset.UTC);
    private static final Acct ALICE_ACCT = new Acct("alice", "mastodon.social");
    private static final Acct EVIL_ACCT = new Acct("alice", "evil.example");
    private static final URI ALICE = URI.create("https://mastodon.social/users/alice");
    private static final URI ALICE_MIXED_CASE = URI.create("https://Mastodon.Social/users/alice");
    private static final URI ALICE_RELATIVE = URI.create("/users/alice");
    private static final URI ALICE_INBOX = URI.create("https://mastodon.social/users/alice/inbox");
    private static final URI ALICE_AVATAR = URI.create("https://mastodon.social/avatars/alice.png");

    @Autowired
    RemoteAccountRepository remoteAccountRepository;

    @Autowired
    JdbcClient jdbcClient;

    private final WebFingerClient webFingerClient = mock(WebFingerClient.class);
    private final RemoteActorFetcher remoteActorFetcher = mock(RemoteActorFetcher.class);

    AccountLookupService accountLookupService;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM follows").update();
        jdbcClient.sql("DELETE FROM remote_accounts").update();
        accountLookupService = new AccountLookupService(
                webFingerClient, remoteActorFetcher, remoteAccountRepository, CLOCK);
    }

    @Test
    void looksUpAndStoresTheAccount() {
        when(webFingerClient.resolve(ALICE_ACCT))
                .thenReturn(new WebFingerAccount(new Acct("Alice", "mastodon.social"), ALICE));
        when(remoteActorFetcher.fetch(ALICE))
                .thenReturn(new RemoteActor(ALICE, ALICE_INBOX, "Alice Liddell", ALICE_AVATAR));

        RemoteAccount result = accountLookupService.lookup(ALICE_ACCT);

        assertThat(remoteAccountRepository.findByActorId(ALICE)).contains(result);
        assertThat(result).isEqualTo(new RemoteAccount(
                result.id(),
                ALICE,
                "Alice@mastodon.social",
                ALICE_INBOX,
                "Alice Liddell",
                ALICE_AVATAR,
                Instant.parse("2026-10-07T10:00:00.123456Z")));
    }

    @Test
    void rejectsAnActorOnAnotherHost() {
        when(webFingerClient.resolve(EVIL_ACCT))
                .thenReturn(new WebFingerAccount(EVIL_ACCT, ALICE));

        assertThatThrownBy(() -> accountLookupService.lookup(EVIL_ACCT))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessage(
                        "WebFinger for alice@evil.example points to an actor on another host: " +
                                "https://mastodon.social/users/alice");
        verifyNoInteractions(remoteActorFetcher);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM remote_accounts").query(Long.class).single()).isZero();
    }

    @Test
    void acceptsAnActorWhoseHostDiffersOnlyInCase() {
        when(webFingerClient.resolve(ALICE_ACCT))
                .thenReturn(new WebFingerAccount(ALICE_ACCT, ALICE_MIXED_CASE));
        when(remoteActorFetcher.fetch(ALICE_MIXED_CASE))
                .thenReturn(new RemoteActor(ALICE_MIXED_CASE, ALICE_INBOX, "Alice Liddell", ALICE_AVATAR));

        accountLookupService.lookup(ALICE_ACCT);

        assertThat(jdbcClient.sql("SELECT actor_id FROM remote_accounts")
                .query(String.class)
                .optional())
                .contains("https://Mastodon.Social/users/alice");
    }

    @Test
    void passesOnAWebFingerFailure() {
        RemoteFetchException failure = new RemoteFetchException("Cannot fetch WebFinger for alice@mastodon.social");
        when(webFingerClient.resolve(ALICE_ACCT)).thenThrow(failure);

        assertThatThrownBy(() -> accountLookupService.lookup(ALICE_ACCT))
                .isSameAs(failure);
        verifyNoInteractions(remoteActorFetcher);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM remote_accounts").query(Long.class).single()).isZero();
    }

    @Test
    void passesOnAnActorFetchFailure() {
        RemoteFetchException failure = new RemoteFetchException("Cannot fetch https://mastodon.social/users/alice");
        when(webFingerClient.resolve(ALICE_ACCT))
                .thenReturn(new WebFingerAccount(ALICE_ACCT, ALICE));
        when(remoteActorFetcher.fetch(ALICE))
                .thenThrow(failure);

        assertThatThrownBy(() -> accountLookupService.lookup(ALICE_ACCT))
                .isSameAs(failure);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM remote_accounts").query(Long.class).single()).isZero();
    }

    @Test
    void rejectsAnActorUrlWithoutHost() {
        when(webFingerClient.resolve(ALICE_ACCT))
                .thenReturn(new WebFingerAccount(ALICE_ACCT, ALICE_RELATIVE));

        assertThatThrownBy(() -> accountLookupService.lookup(ALICE_ACCT))
                .isInstanceOf(RemoteFetchException.class)
                .hasMessage("WebFinger for alice@mastodon.social points to an actor on another host: /users/alice");
        verifyNoInteractions(remoteActorFetcher);
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM remote_accounts").query(Long.class).single()).isZero();
    }
}
