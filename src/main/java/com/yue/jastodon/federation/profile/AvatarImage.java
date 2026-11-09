package com.yue.jastodon.federation.profile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

final class AvatarImage {

    private AvatarImage() {}

    static byte[] normalise(byte[] upload) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(upload));
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            return png.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
