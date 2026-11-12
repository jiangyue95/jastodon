package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3AvatarStorageTest {

    private static final UUID ID = UUID.fromString("3f2504e0-4f89-41d3-9a0c-0305e82c3301");
    private static final UUID OTHER_ID = UUID.fromString("9b2c7e41-0d5a-4f3e-8a61-2f7c4b1d6e90");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private final S3Client s3Client = mock(S3Client.class);
    private final S3AvatarStorage storage = new S3AvatarStorage(
            s3Client,
            new MediaStorageProperties(
                    "jastodon-media-test",
                    "eu-west-1",
                    "test-access-key-id",
                    "test-secret-access-key"),
            List.of(ID, OTHER_ID).iterator()::next);

    @Test
    void storesThePngUnderANewKey() throws IOException {
        String key = storage.store(PNG);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(request.capture(), body.capture());

        assertThat(request.getValue()).isEqualTo(PutObjectRequest.builder()
                .bucket("jastodon-media-test")
                .key("avatars/3f2504e0-4f89-41d3-9a0c-0305e82c3301.png")
                .ifNoneMatch("*")
                .contentType("image/png")
                .cacheControl("public, max-age=31536000, immutable")
                .build());
        try (InputStream in = body.getValue().contentStreamProvider().newStream()) {
            assertThat(in.readAllBytes()).isEqualTo(PNG);
        }
        assertThat(key).isEqualTo("avatars/3f2504e0-4f89-41d3-9a0c-0305e82c3301.png");
    }
}
