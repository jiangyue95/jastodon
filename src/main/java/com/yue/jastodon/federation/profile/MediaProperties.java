package com.yue.jastodon.federation.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("jastodon.media")
record MediaProperties(URI baseUrl) {
}
