package com.yue.jastodon.federation.profile;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MediaStorageConfigurationTest {

    @Test
    void usesTheConfiguredRegionAndCredentials() {
        MediaStorageProperties properties = new MediaStorageProperties(
                "jastodon-media-test",
                "eu-west-1",
                "test-access-key-id",
                "test-secret-access-key");

        try (S3Client client = new MediaStorageConfiguration().mediaS3Client(properties)) {
            var configuration = client.serviceClientConfiguration();
            var credentials = configuration.credentialsProvider().resolveIdentity().join();

            assertThat(configuration.region()).isEqualTo(Region.EU_WEST_1);
            assertThat(credentials.accessKeyId()).isEqualTo("test-access-key-id");
            assertThat(credentials.secretAccessKey()).isEqualTo("test-secret-access-key");

            assertThat(configuration.overrideConfiguration().apiCallTimeout()).contains(Duration.ofSeconds(30));
        }
    }
}
