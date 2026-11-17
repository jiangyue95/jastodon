package com.yue.jastodon.federation.note;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

record NoteCursor(Instant published, UUID id) {

    String encode() {
        String raw = published.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static Optional<NoteCursor> decode(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            String raw = new String(bytes, StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            if (parts.length != 2) {
                return Optional.empty();
            }
            Instant published = Instant.parse(parts[0]);
            UUID id = UUID.fromString(parts[1]);
            return Optional.of(new NoteCursor(published, id));
        } catch (IllegalArgumentException | DateTimeException e) {
            return Optional.empty();
        }
    }
}
