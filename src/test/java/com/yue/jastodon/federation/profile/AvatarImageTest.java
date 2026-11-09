package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarImageTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    @Test
    void acceptsA400By400Png() throws IOException {
        byte[] result = AvatarImage.normalise(image(400, 400, "png"));

        BufferedImage read = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(read).isNotNull();
        assertThat(read.getWidth()).isEqualTo(400);
        assertThat(read.getHeight()).isEqualTo(400);
    }

    @Test
    void convertsAJpegToPng() throws IOException {
        byte[] result = AvatarImage.normalise(image(400, 400, "jpeg"));

        assertThat(result).startsWith(PNG_SIGNATURE);
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(read).isNotNull();
        assertThat(read.getWidth()).isEqualTo(400);
        assertThat(read.getHeight()).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gif", "bmp", "tiff"})
    void rejectsOtherFormats(String format) {
        byte[] upload = image(400, 400, format);

        assertThatThrownBy(() -> AvatarImage.normalise(upload))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Avatar must be a PNG or JPEG image");
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", ""})
    void rejectsBytesThatAreNotAnImage(String content) {
        byte[] upload = content.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> AvatarImage.normalise(upload))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Avatar must be a PNG or JPEG image");
    }

    @Test
    void rejectsABrokenPng() {
        byte[] png = image(400, 400, "png");
        byte[] upload = Arrays.copyOf(png, png.length / 2);

        assertThatThrownBy(() -> AvatarImage.normalise(upload))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Cannot read image")
                .hasCauseInstanceOf(IOException.class);
    }

    private static byte[] image(int width, int height, String format) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, format, out)) {
                throw new IllegalStateException("No ImageIO write for " + format);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
