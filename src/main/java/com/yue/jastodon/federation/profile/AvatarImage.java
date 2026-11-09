package com.yue.jastodon.federation.profile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

final class AvatarImage {

    private AvatarImage() {}

    static byte[] normalise(byte[] upload) {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(upload))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            try {
                if (!isAllowed(reader.getFormatName())) {
                    throw new InvalidImageException("Avatar must be a PNG or JPEG image");
                }
                reader.setInput(input);
                BufferedImage image = reader.read(0);
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(image, "png", png);
                return png.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isAllowed(String formatName) {
        return formatName.equalsIgnoreCase("png") || formatName.equalsIgnoreCase("jpeg");
    }
}
