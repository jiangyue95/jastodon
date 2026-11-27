package com.yue.jastodon.federation.following;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FollowControllerTest extends AbstractIntegrationTest {

    private static final URI FOLLOW_ID = URI.create("https://jastodon.test/users/yue#follows/2ba78572-9429-4cd3-ab4a-bd82f9a5659b");
    private static final Instant REQUESTED = Instant.parse("2026-10-07T10:00:00Z");
    private static final Instant ACCEPTED = Instant.parse("2026-10-07T10:05:00Z");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    FollowService followService;

    @Test
    void followsAnAccount() throws Exception {
        when(followService.follow(42)).thenReturn(Optional.of(new FollowResult(
                new Follow(7, 42, FOLLOW_ID, FollowState.PENDING, REQUESTED, null),
                true)));

        String body = mockMvc.perform(put("/following/42")
                        .with(oauth2Login())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                {
                  "accountId": 42,
                  "state": "PENDING"
                }
                """));
    }

    @Test
    void answersOkWhenTheAccountIsAlreadyFollowed() throws Exception {
        when(followService.follow(42)).thenReturn(Optional.of(new FollowResult(
                new Follow(7, 42, FOLLOW_ID, FollowState.ACCEPTED, REQUESTED, ACCEPTED),
                false)));

        String body = mockMvc.perform(put("/following/42")
                        .with(oauth2Login())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                {
                  "accountId": 42,
                  "state": "ACCEPTED"
                }
                """));
    }

    @Test
    void answersNotFoundForAnUnknownAccount() throws Exception {
        when(followService.follow(42)).thenReturn(Optional.empty());

        mockMvc.perform(put("/following/42")
                        .with(oauth2Login())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        verify(followService).follow(42);
    }

    @Test
    void rejectsANonNumericId() throws Exception {
        mockMvc.perform(put("/following/abc")
                        .with(oauth2Login())
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(followService);
    }

    @Test
    void answersUnauthorizedToAnAnonymousRequestWithACsrfToken() throws Exception {
        mockMvc.perform(put("/following/42")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verifyNoInteractions(followService);
    }

    @Test
    void rejectsALoggedInRequestWithoutACsrfToken() throws Exception {
        mockMvc.perform(put("/following/42")
                        .with(oauth2Login()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(followService);;
    }
}
