package com.yue.jastodon.federation.note;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NoteCursorTest {

    private static final NoteCursor CURSOR = new NoteCursor(
            Instant.parse("2026-11-07T23:48:55.494691Z"),
            UUID.fromString("8da406ec-1311-4346-87c6-9d39e0eefab5"));

    // URL-safe Base64, no padding, of "2026-11-07T23:48:55.494691Z|8da406ec-1311-4346-87c6-9d39e0eefab5"
    private static final String ENCODED =
            "MjAyNi0xMS0wN1QyMzo0ODo1NS40OTQ2OTFafDhkYTQwNmVjLTEzMTEtNDM0Ni04N2M2LTlkMzllMGVlZmFiNQ";

    @Test
    void encodesThePublishedTimeAndIdAsUrlSafeBase64() {
        assertThat(CURSOR.encode()).isEqualTo(ENCODED);
    }

    @Test
    void decodesAnEncodedCursor() {
        assertThat(NoteCursor.decode(ENCODED)).contains(CURSOR);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "!!!",
            "aGVsbG8",
            "bm90LWEtdGltZXw4ZGE0MDZlYy0xMzExLTQzNDYtODdjNi05ZDM5ZTBlZWZhYjU",
            "MjAyNi0xMS0wN1QyMzo0ODo1NS40OTQ2OTFafG5vdC1hLXV1aWQ",
            "MjAyNi0xMS0wN1QyMzo0ODo1NS40OTQ2OTFa",
            "MjAyNi0xMS0wN1QyMzo0ODo1NS40OTQ2OTFafDhkYTQwNmVjLTEzMTEtNDM0Ni04N2M2LTlkMzllMGVlZmFiNXxleHRyYQ"})
    void rejectsAMalformedCursor(String value) {
        assertThat(NoteCursor.decode(value)).isEmpty();
    }
}
