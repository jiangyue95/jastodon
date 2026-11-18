package com.yue.jastodon.federation.actor;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest extends AbstractIntegrationTest {

    private static final String YUE_AVATAR_KEY = "avatars/9b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e.png";
    private static final String ALICE_AVATAR_KEY = "avatars/1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.png";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM profiles").update();
    }

    @Test
    void servesTheAvatarUrl() throws Exception {
        saveAvatarKey("yue", YUE_AVATAR_KEY);

        String body = mockMvc.perform(get("/profile")
                        .with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                {
                  "avatarUrl": "https://media.jastodon.test/avatars/9b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e.png"
                }
                """));
    }

    @Test
    void answersNullWithoutAnAvatar() throws Exception {
        saveAvatarKey("alice", ALICE_AVATAR_KEY);

        String body = mockMvc.perform(get("/profile")
                        .with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                {
                  "avatarUrl": null
                }
                """));
    }

    @Test
    void answersUnauthorizedToAnAnonymousRequest() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    private void saveAvatarKey(String username, String avatarKey) {
        jdbcClient.sql("INSERT INTO profiles (username, avatar_key) VALUES (:username, :avatarKey)")
                .param("username", username)
                .param("avatarKey", avatarKey)
                .update();
    }
}
