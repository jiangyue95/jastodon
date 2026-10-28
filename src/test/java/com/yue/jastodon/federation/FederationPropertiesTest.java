package com.yue.jastodon.federation;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FederationPropertiesTest {

    private final FederationProperties properties = new FederationProperties(
            "yue",
            URI.create("https://jastodon.test"),
            new ByteArrayResource(new byte[0]));

    @Test
    void noteIdIsUnderTheActorsStatuses() {
        UUID id = UUID.fromString("0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");

        assertThat(properties.noteId(id)).isEqualTo(
                URI.create("https://jastodon.test/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69"));
    }

    @Test
    void noteIdRejectsNull() {
        assertThatThrownBy(() -> properties.noteId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Note id must not be null");
    }
}
