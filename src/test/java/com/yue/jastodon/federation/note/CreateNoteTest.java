package com.yue.jastodon.federation.note;

import com.yue.jastodon.federation.FederationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateNoteTest {

    private static final UUID ID = UUID.fromString("0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69");

    private final FederationProperties federation = new FederationProperties(
            "yue",
            URI.create("https://jastodon.test"),
            new ByteArrayResource(new byte[0]));

    @Test
    void serialisesLikeMastodonCreate() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        Note note = new Note(ID, "a < b\nc", Instant.parse("2026-10-07T10:00:00Z"));
        NoteDocument noteDocument = NoteDocument.of(note, federation);

        String json = jsonMapper.writeValueAsString(CreateNote.of(noteDocument));

        assertThat(jsonMapper.readTree(json)).isEqualTo(jsonMapper.readTree("""
                {
                  "@context": "https://www.w3.org/ns/activitystreams",
                  "id": "https://jastodon.test/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69/activity",
                  "type": "Create",
                  "actor": "https://jastodon.test/users/yue",
                  "published": "2026-10-07T10:00:00Z",
                  "to": ["https://www.w3.org/ns/activitystreams#Public"],
                  "object": {
                    "id": "https://jastodon.test/users/yue/statuses/0e1f6a3c-5b2d-4c8e-9a7f-1d2e3c4b5a69",
                    "type": "Note",
                    "attributedTo": "https://jastodon.test/users/yue",
                    "content": "<p>a &lt; b<br />c</p>",
                    "published": "2026-10-07T10:00:00Z",
                    "to": ["https://www.w3.org/ns/activitystreams#Public"]
                  }
                }
                """));
    }
}
