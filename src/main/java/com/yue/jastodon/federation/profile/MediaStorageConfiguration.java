package com.yue.jastodon.federation.profile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

@Configuration
class MediaStorageConfiguration {

    @Bean
    S3Client mediaS3Client(MediaStorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())))
                .httpClient(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5))
                        .socketTimeout(Duration.ofSeconds(10))
                        .build())
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(30)))
                .build();
    }
}
