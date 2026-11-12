package com.yue.jastodon.federation.profile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
class MediaStorageConfiguration {

    @Bean
    S3Client mediaS3Client(MediaStorageProperties properties) {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials.create("stub", "stub")))
                .build();
    }
}
