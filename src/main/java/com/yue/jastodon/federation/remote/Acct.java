package com.yue.jastodon.federation.remote;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record Acct(String username, String host) {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]+([.-]+[A-Za-z0-9_]+)*");
    private static final Pattern HOST = Pattern.compile("[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+");

    public Acct {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException("Invalid username: " + username);
        }
        if (host == null || !HOST.matcher(host).matches()) {
            throw new IllegalArgumentException("Invalid host: " + host);
        }
        host = host.toLowerCase(Locale.ROOT);
    }

    public static Optional<Acct> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String handle = input.strip();
        if (handle.startsWith("@")) {
            handle = handle.substring(1);
        }
        int at = handle.indexOf('@');
        if (at <= 0 || at != handle.lastIndexOf('@') || at == handle.length() - 1) {
            return Optional.empty();
        }
        String username = handle.substring(0, at);
        String host = handle.substring(at + 1);
        try {
            return Optional.of(new Acct(username, host));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String value() {
        return username + "@" + host;
    }
}
