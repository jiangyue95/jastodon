package com.yue.jastodon.federation.profile;

import org.springframework.stereotype.Component;
import org.springframework.util.IdGenerator;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
class S3AvatarStorage implements AvatarStorage {

    private static final String CONTENT_TYPE = "image/png";
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3Client;
    private final MediaStorageProperties properties;
    private final IdGenerator idGenerator;

    S3AvatarStorage(S3Client s3Client, MediaStorageProperties properties, IdGenerator idGenerator) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.idGenerator = idGenerator;
    }

    @Override
    public String store(byte[] png) {
        String key = "avatars/" + idGenerator.generateId() + ".png";
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .ifNoneMatch("*")
                .contentType(CONTENT_TYPE)
                .cacheControl(CACHE_CONTROL)
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(png));
        } catch (SdkException e) {
            throw new AvatarStorageException("Cannot store avatar " + key, e);
        }
        return key;
    }
}
