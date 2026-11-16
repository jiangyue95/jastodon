package com.yue.jastodon.federation.note;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NoteListTest extends AbstractIntegrationTest {

    private static final UUID OLDER_ID = UUID.fromString("0e1f2a3b-4c5d-4e6f-8a9b-0c1d2e3f4a5b");
    private static final UUID NEWER_ID = UUID.fromString("9b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM notes").update();
        noteRepository.save(new Note(OLDER_ID, "older", Instant.parse("2026-10-07T10:00:00Z")));
        noteRepository.save(new Note(NEWER_ID, "a < b\nc", Instant.parse("2026-10-07T11:00:00Z")));
    }

    @Test
    void listsTheNotesNewestFirst() throws Exception {
        String body = mockMvc.perform(get("/notes")
                .with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                {
                  "notes": [
                    {
                      "id": "9b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e",
                      "content": "a < b\\nc",
                      "published": "2026-10-07T11:00:00Z",
                      "url": "https://jastodon.test/users/yue/statuses/9b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e"
                    },
                    {
                      "id": "0e1f2a3b-4c5d-4e6f-8a9b-0c1d2e3f4a5b",
                      "content": "older",
                      "published": "2026-10-07T10:00:00Z",
                      "url": "https://jastodon.test/users/yue/statuses/0e1f2a3b-4c5d-4e6f-8a9b-0c1d2e3f4a5b"
                    }
                  ]
                }
                """));
    }

    @Test
    void answersUnauthorizedToAnAnonymousRequest() throws Exception {
        mockMvc.perform(get("/notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }
}
