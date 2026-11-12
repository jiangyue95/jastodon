package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaStoragePropertiesTest {

    private static final String BUCKET = "jastodon-media-test";
    private static final String REGION = "eu-west-1";
    private static final String ACCESS_KEY_ID = "test-access-key-id";
    private static final String SECRET_ACCESS_KEY = "test-secret-access-key";

    @Test
    void keepsAllValues() {
        MediaStorageProperties properties =
                new MediaStorageProperties(BUCKET, REGION, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        assertThat(properties.bucket()).isEqualTo("jastodon-media-test");
        assertThat(properties.region()).isEqualTo("eu-west-1");
        assertThat(properties.accessKeyId()).isEqualTo("test-access-key-id");
        assertThat(properties.secretAccessKey()).isEqualTo("test-secret-access-key");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsABlankBucket(String bucket) {
        assertThatThrownBy(() -> new MediaStorageProperties(bucket, REGION, ACCESS_KEY_ID, SECRET_ACCESS_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jastodon.media.storage.bucket must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsABlankRegion(String region) {
        assertThatThrownBy(() -> new MediaStorageProperties(BUCKET, region, ACCESS_KEY_ID, SECRET_ACCESS_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jastodon.media.storage.region must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsABlankAccessKeyId(String accessKeyId) {
        assertThatThrownBy(() -> new MediaStorageProperties(BUCKET, REGION, accessKeyId, SECRET_ACCESS_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jastodon.media.storage.access-key-id must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsABlankSecretAccessKey(String secretAccessKey) {
        assertThatThrownBy(() -> new MediaStorageProperties(BUCKET, REGION, ACCESS_KEY_ID, secretAccessKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("jastodon.media.storage.secret-access-key must not be blank");
    }

    @Test
    void toStringHidesTheSecret() {
        MediaStorageProperties properties =
                new MediaStorageProperties(BUCKET, REGION, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        assertThat(properties.toString())
                .isEqualTo("MediaStorageProperties[bucket=jastodon-media-test, region=eu-west-1, " +
                        "accessKeyId=test-access-key-id, secretAccessKey=***]")
                .doesNotContain("test-secret-access-key");
    }
}
