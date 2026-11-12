package com.yue.jastodon.federation.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jastodon.media.storage")
record MediaStorageProperties(String bucket, String region, String accessKeyId, String secretAccessKey) {

    MediaStorageProperties {
        requireText(bucket, "bucket");
        requireText(region, "region");
        requireText(accessKeyId, "access-key-id");
        requireText(secretAccessKey, "secret-access-key");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("jastodon.media.storage." + name + " must not be blank");
        }
    }

    @Override
    public String toString() {
        return "MediaStorageProperties[bucket=" + bucket
                + ", region=" + region
                + ", accessKeyId=" + accessKeyId
                + ", secretAccessKey=***]";
    }
}
