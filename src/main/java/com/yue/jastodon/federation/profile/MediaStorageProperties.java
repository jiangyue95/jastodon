package com.yue.jastodon.federation.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jastodon.media.storage")
record MediaStorageProperties(String bucket, String region, String accessKeyId, String secretAccessKey) {
}
