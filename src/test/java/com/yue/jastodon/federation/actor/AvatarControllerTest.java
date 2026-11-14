package com.yue.jastodon.federation.actor;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AvatarControllerTest extends AbstractIntegrationTest {

    private static final byte[] IMAGE = {1, 2, 3};

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AvatarService avatarService;

    @Test
    void changesTheAvatar() throws Exception {
        when(avatarService.change(any())).thenReturn(
                URI.create("https://media.jastodon.test/avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png"));

        mockMvc.perform(multipart("/profile/avatar")
                .file(new MockMultipartFile("avatar", "avatar.png", "image/png", IMAGE))
                .with(oauth2Login())
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.url").value(
                        "https://media.jastodon.test/avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png"));

        verify(avatarService).change(IMAGE);
    }
}
