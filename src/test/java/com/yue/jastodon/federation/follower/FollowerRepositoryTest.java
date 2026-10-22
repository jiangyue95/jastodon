package com.yue.jastodon.federation.follower;

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
class FollowerRepositoryTest extends AbstractIntegrationTest {

    private static final Instant FIRST = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    FollowerRepository followerRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void emptyTable() {
        jdbcClient.sql("DELETE FROM followers").update();
    }

    @Test
    void savesAndListsFollower() {
        Follower alice = new Follower(
                URI.create("https://mastodon.example/users/alice"),
                URI.create("https://mastodon.example/users/alice/inbox"),
                URI.create("https://mastodon.example/activities/follow-1"),
                FIRST);

        followerRepository.save(alice);

        assertThat(followerRepository.findAll()).containsExactly(alice);
    }

    @Test
    void savingTheSameFollowTwiceKeepsOneRow() {
        Follower alice = new Follower(
                URI.create("https://mastodon.example/users/alice"),
                URI.create("https://mastodon.example/users/alice/inbox"),
                URI.create("https://mastodon.example/activities/follow-1"),
                FIRST);

        followerRepository.save(alice);
        followerRepository.save(alice);

        assertThat(followerRepository.findAll()).containsExactly(alice);
    }

    @Test
    void refollowUpdatesInboxAndFollowIdButKeepsFollowedAt() {
        Follower alice = new Follower(
                URI.create("https://mastodon.example/users/alice"),
                URI.create("https://mastodon.example/users/alice/inbox"),
                URI.create("https://mastodon.example/activities/follow-1"),
                FIRST);
        Follower refollow = new Follower(
                URI.create("https://mastodon.example/users/alice"),
                URI.create("https://mastodon.example/users/alice/new-inbox"),
                URI.create("https://mastodon.example/activities/follow-2"),
                LATER);

        followerRepository.save(alice);
        followerRepository.save(refollow);

        Follower expected = new Follower(
                URI.create("https://mastodon.example/users/alice"),
                URI.create("https://mastodon.example/users/alice/new-inbox"),
                URI.create("https://mastodon.example/activities/follow-2"),
                FIRST);

        assertThat(followerRepository.findAll()).containsExactly(expected);
    }

    @Test
    void listsFollowersInTheOrderTheyFollowed() {
        Follower bob = new Follower(
                URI.create("https://mastodon.example/users/bob"),
                URI.create("https://mastodon.example/users/bob/inbox"),
                URI.create("https://mastodon.example/activities/follow-1"),
                LATER);
        Follower alice = new Follower(
                URI.create("https://mastodon.example/users/alice"),
                URI.create("https://mastodon.example/users/alice/inbox"),
                URI.create("https://mastodon.example/activities/follow-1"),
                FIRST);

        followerRepository.save(bob);
        followerRepository.save(alice);

        assertThat(followerRepository.findAll()).containsExactly(alice, bob);
    }
}
