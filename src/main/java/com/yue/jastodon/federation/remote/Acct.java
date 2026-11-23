package com.yue.jastodon.federation.remote;

import java.util.Optional;

public record Acct(String username, String host) {

    public static Optional<Acct> parse(String input) {
        return Optional.empty();
    }

    public String value() {
        return null;
    }
}
