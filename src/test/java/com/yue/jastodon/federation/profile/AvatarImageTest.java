package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarImageTest {

    @Test
    void acceptsA400By400Png() throws IOException {
        byte[] result = AvatarImage.normalise(image(400, 400, "png"));

        BufferedImage read = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(read).isNotNull();
        assertThat(read.getWidth()).isEqualTo(400);
        assertThat(read.getHeight()).isEqualTo(400);
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
