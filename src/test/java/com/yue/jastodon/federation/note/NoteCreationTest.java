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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NoteCreationTest extends AbstractIntegrationTest {

    private static final String BODY = """
            {"content": "hello"}
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    NoteRepository noteRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM notes").update();
    }

    @Test
    void createsANote() throws Exception {
        String location = mockMvc.perform(post("/notes")
                .with(oauth2Login())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/activity+json"))
                .andExpect(jsonPath("$.type").value("Note"))
                .andExpect(jsonPath("$.content").value("<p>hello</p>"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        assertThat(noteRepository.findAll()).singleElement().satisfies(note -> {
            assertThat(note.content()).isEqualTo("hello");
            assertThat(location).isEqualTo("https://jastodon.test/users/yue/statuses/" + note.id());
        });
    }

    @Test
    void rejectsALoggedInPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/notes")
                .with(oauth2Login())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isForbidden());

        assertThat(noteRepository.findAll()).isEmpty();
    }

    @Test
    void rejectsAnAnonymousPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().isForbidden());

        assertThat(noteRepository.findAll()).isEmpty();
    }

    @Test
    void redirectsAnAnonymousPostWithACsrfToken() throws Exception {
        mockMvc.perform(post("/notes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
                .andExpect(status().is3xxRedirection());

        assertThat(noteRepository.findAll()).isEmpty();
    }
}
