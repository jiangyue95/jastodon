package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

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

    @ParameterizedTest
    @CsvSource({"png, 401, 400", "png, 400, 401", "jpeg, 401, 400"})
    void rejectsOtherSizes(String format, int width, int height) {
        byte[] upload = image(width, height, format);

        assertThatThrownBy(() -> AvatarImage.normalise(upload))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Avatar must be 400x400, was " + width + "x" + height);
    }

    @Test
    void rejectsAnOversizedHeaderBeforeDecoding() {
        byte[] upload = image(400, 400, "png");
        ByteBuffer bytes = ByteBuffer.wrap(upload);
        bytes.putInt(16, 50_000);
        bytes.putInt(20, 50_000);
        CRC32 crc = new CRC32();
        crc.update(upload, 12, 17);
        bytes.putInt(29, (int) crc.getValue());

        assertThatThrownBy(() -> AvatarImage.normalise(upload))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("Avatar must be 400x400, was 50000x50000");
    }

    @Test
    void dropsMetadata() {
        byte[] upload = withTextChunk(image(400, 400, "png"), "comment", "taken at home");
        assertThat(latin1(upload)).contains("taken at home");

        byte[] result = AvatarImage.normalise(upload);

        assertThat(latin1(result)).doesNotContain("taken at home");
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

    private static byte[] withTextChunk(byte[] png, String keyword, String text) {
        byte[] data = (keyword + "\0" + text).getBytes(StandardCharsets.ISO_8859_1);
        ByteBuffer chunk = ByteBuffer.allocate(12 + data.length);
        chunk.putInt(data.length);
        chunk.put("tEXt".getBytes(StandardCharsets.US_ASCII));
        chunk.put(data);
        CRC32 crc = new CRC32();
        crc.update(chunk.array(), 4, 4 + data.length);
        chunk.putInt((int) crc.getValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(png, 0, 33);
        out.write(chunk.array(), 0, chunk.capacity());
        out.write(png, 33, png.length - 33);
        return out.toByteArray();
    }

    private static String latin1(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
