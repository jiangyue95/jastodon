package com.yue.jastodon.federation.following;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.remote.Acct;
import com.yue.jastodon.federation.remote.RemoteFetchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AccountLookupControllerTest extends AbstractIntegrationTest {

    private static final Acct ALICE_ACCT = new Acct("alice", "mastodon.social");
    private static final RemoteAccount ALICE = new RemoteAccount(
            42,
            URI.create("https://mastodon.social/users/alice"),
            "alice@mastodon.social",
            URI.create("https://mastodon.social/users/alice/inbox"),
            "Alice Liddell",
            URI.create("https://files.mastodon.social/avatars/alice.png"),
            Instant.parse("2026-10-07T10:00:00Z"));

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    AccountLookupService accountLookupService;

    @Test
    void returnsTheAccount() throws Exception {
        when(accountLookupService.lookup(ALICE_ACCT)).thenReturn(ALICE);

        String body = mockMvc.perform(get("/accounts/lookup")
                        .param("acct", "@alice@mastodon.social")
                        .with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                {
                  "id": 42,
                  "acct": "alice@mastodon.social",
                  "name": "Alice Liddell",
                  "avatarUrl": "https://files.mastodon.social/avatars/alice.png"
                }
                """));
    }

    @Test
    void answersNullsWithoutNameOrAvatar() throws Exception {
        when(accountLookupService.lookup(ALICE_ACCT)).thenReturn(new RemoteAccount(
                42,
                URI.create("https://mastodon.social/users/alice"),
                "alice@mastodon.social",
                URI.create("https://mastodon.social/users/alice/inbox"),
                null,
                null,
                Instant.parse("2026-10-07T10:00:00Z")));

        String body = mockMvc.perform(get("/accounts/lookup")
                        .param("acct", "@alice@mastodon.social")
                        .with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(jsonMapper.readTree(body)).isEqualTo(jsonMapper.readTree("""
                {
                  "id": 42,
                  "acct": "alice@mastodon.social",
                  "name": null,
                  "avatarUrl": null
                }
                """));
    }

    @Test
    void rejectsAnInvalidHandle() throws Exception {
        mockMvc.perform(get("/accounts/lookup")
                        .param("acct", "alice")
                        .with(oauth2Login()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));

        verifyNoInteractions(accountLookupService);
    }

    @Test
    void rejectsAHandleOnThisServer() throws Exception {
        mockMvc.perform(get("/accounts/lookup")
                        .param("acct", "alice@Jastodon.Test")
                        .with(oauth2Login()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));

        verifyNoInteractions(accountLookupService);
    }

    @Test
    void answersNotFoundWhenTheLookupFails(CapturedOutput output) throws Exception {
        when(accountLookupService.lookup(ALICE_ACCT)).thenThrow(new RemoteFetchException(
                "Cannot fetch https://mastodon.social/.well-known/webfinger?resource=acct:alice@mastodon.social"));

        mockMvc.perform(get("/accounts/lookup")
                        .param("acct", "alice@mastodon.social")
                        .with(oauth2Login()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        assertThat(output).contains(
                "Account lookup answered 404: Cannot fetch " +
                        "https://mastodon.social/.well-known/webfinger?resource=acct:alice@mastodon.social");
    }

    @Test
    void rejectsAMissingHandle() throws Exception {
        mockMvc.perform(get("/accounts/lookup")
                        .with(oauth2Login()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountLookupService);
    }

    @Test
    void answersUnauthorizedToAnAnonymousRequest() throws Exception {
        mockMvc.perform(get("/accounts/lookup")
                        .param("acct", "alice@mastodon.social"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verifyNoInteractions(accountLookupService);
    }
}
