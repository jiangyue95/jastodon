package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaPropertiesTest {

    private static final String MESSAGE = "jastodon.media.base-url must be an https origin with no path, e.g. " +
            "https://media.example.com";

    @Test
    void acceptsAnHttpsOrigin() {
        assertThat(new MediaProperties(URI.create("https://media.jastodon.test")).baseUrl())
                .isEqualTo(URI.create("https://media.jastodon.test"));
    }

    @Test
    void rejectsAMissingBaseUrl() {
        assertThatThrownBy(() -> new MediaProperties(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://media.jastodon.test",
            "https://media.jastodon.test/",
            "https://media.jastodon.test/avatars",
            "https://media.jastodon.test?v=1",
            "https://media.jastodon.test#top",
            "https://user@media.jastodon.test",
            "https:media.jastodon.test",
            "media.jastodon.test"
    })
    void rejectsAnythingButAnHttpsOrigin(String baseUrl) {
        assertThatThrownBy(() -> new MediaProperties(URI.create(baseUrl)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MESSAGE);
    }
}
