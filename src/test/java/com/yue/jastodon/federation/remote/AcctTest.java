package com.yue.jastodon.federation.remote;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcctTest {

    @Test
    void parsesAUserAtAHost() {
        assertThat(Acct.parse("alice@mastodon.social"))
                .contains(new Acct("alice", "mastodon.social"));
    }

    @Test
    void acceptsALeadingAt() {
        assertThat(Acct.parse("@alice@mastodon.social"))
                .contains(new Acct("alice", "mastodon.social"));
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertThat(Acct.parse("\u3000alice@mastodon.social "))
                .contains(new Acct("alice", "mastodon.social"));
    }

    @Test
    void lowerCasesTheHostAndKeepsTheUsername() {
        assertThat(Acct.parse("Alice@Mastodon.Social"))
                .contains(new Acct("Alice", "mastodon.social"));
    }

    @Test
    void acceptsDotsAndDashesInsideAUsername() {
        assertThat(Acct.parse("first.last-name_1@mastodon.social"))
                .contains(new Acct("first.last-name_1", "mastodon.social"));
    }

    @Test
    void valueIsTheUserAtTheHost() {
        assertThat(new Acct("alice", "mastodon.social").value())
                .isEqualTo("alice@mastodon.social");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "alice",
            "@alice",
            "alice@",
            "@@alice@mastodon.social",
            "alice@bob@mastodon.social",
            "alice@mastodon.social/path",
            "alice@mastodon.social:8443",
            "alice@localhost",
            "ali ce@mastodon.social",
            "alice?x@mastodon.social",
            ".alice@mastodon.social",
            "alice.@mastodon.social",
            "alice@mastodon..social"
    })
    void rejectsAnInvalidHandle(String input) {
        assertThat(Acct.parse(input)).isEmpty();
    }

    @Test
    void constructorRejectsAnInvalidUsername() {
        assertThatThrownBy(() -> new Acct("ali ce", "mastodon.social"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username: ali ce");
    }

    @Test
    void constructorRejectsAnInvalidHost() {
        assertThatThrownBy(() -> new Acct("alice", "mastodon.social/path"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid host: mastodon.social/path");
    }
}
