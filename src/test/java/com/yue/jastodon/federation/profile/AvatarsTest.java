package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvatarsTest {

    private static final String YUE = "yue";
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final AvatarStorage avatarStorage = mock(AvatarStorage.class);
    private final Avatars avatars = new Avatars(
            profileRepository,
            new MediaProperties(URI.create("https://media.jastodon.test")),
            avatarStorage);

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

    @Test
    void uploadStoresTheImageAsPng() throws IOException {
        when(avatarStorage.store(any(byte[].class)))
                .thenReturn("avatars/3f2504e0-4f89-41d3-9a0c-0305e82c3301.png");

        String key = avatars.upload(image(400, 400, "jpeg"));

        ArgumentCaptor<byte[]> stored = ArgumentCaptor.forClass(byte[].class);
        verify(avatarStorage).store(stored.capture());
        assertThat(stored.getValue()).startsWith(PNG_SIGNATURE);
        assertThat(key).isEqualTo("avatars/3f2504e0-4f89-41d3-9a0c-0305e82c3301.png");
    }

    private static byte[] image(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, out)) {
            throw new IllegalStateException("No ImageIO writer for " + format);
        }
        return out.toByteArray();
    }
}
