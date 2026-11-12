package com.yue.jastodon.federation.profile;

import org.springframework.stereotype.Component;
import org.springframework.util.IdGenerator;
import software.amazon.awssdk.services.s3.S3Client;

@Component
class S3AvatarStorage implements AvatarStorage {

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
        return null;
    }
}
