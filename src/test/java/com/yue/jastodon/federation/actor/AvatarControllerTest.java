package com.yue.jastodon.federation.actor;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.profile.AvatarStorageException;
import com.yue.jastodon.federation.profile.InvalidImageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
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
@ExtendWith(OutputCaptureExtension.class)
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

    @Test
    void rejectsAnInvalidImage(CapturedOutput output) throws Exception {
        when(avatarService.change(any()))
                .thenThrow(new InvalidImageException("Avatar must be a PNG or JPEG image"));

        mockMvc.perform(multipart("/profile/avatar")
                .file(new MockMultipartFile("avatar", "avatar.png", "image/png", IMAGE))
                .with(oauth2Login())
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));

        assertThat(output).contains("Avatar upload answered 400: Avatar must be a PNG or JPEG image");
    }

    @Test
    void answersServiceUnavailableWhenTheAvatarCannotBeStored(CapturedOutput output) throws Exception {
        when(avatarService.change(any())).thenThrow(new AvatarStorageException(
                "Cannot store avatar avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png",
                new IllegalStateException("connection reset")));

        mockMvc.perform(multipart("/profile/avatar")
                .file(new MockMultipartFile("avatar", "avatar.png", "image/png", IMAGE))
                .with(oauth2Login())
                .with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));

        assertThat(output)
                .contains("Avatar upload answered 503: " +
                        "Cannot store avatar avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png")
                .contains("Caused by: java.lang.IllegalStateException: connection reset");
    }
}
