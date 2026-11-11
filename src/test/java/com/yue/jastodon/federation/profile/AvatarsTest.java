package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvatarsTest {

    private static final String YUE = "yue";

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final Avatars avatars = new Avatars(
            profileRepository,
            new MediaProperties(URI.create("https://media.jastodon.test")));

    @Test
    void currentUrlIsTheKeyUnderTheMediaBaseUrl() {
        when(profileRepository.findAvatarKey(YUE))
                .thenReturn(Optional.of("avatars/6f1c2a3e-8b4d-4e7a-9c5f-2d3e4f5a6b7c.png"));

        assertThat(avatars.currentUrl(YUE))
                .hasValue(URI.create("https://media.jastodon.test/avatars/6f1c2a3e-8b4d-4e7a-9c5f-2d3e4f5a6b7c.png"));
    }

    @Test
    void noAvatarWithoutAStoredKey() {
        when(profileRepository.findAvatarKey(YUE)).thenReturn(Optional.empty());

        assertThat(avatars.currentUrl(YUE)).isEmpty();
    }
}
