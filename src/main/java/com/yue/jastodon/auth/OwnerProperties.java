package com.yue.jastodon.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jstodon.owner")
public record OwnerProperties(long githubId) {

    public OwnerProperties {
        if(githubId <= 0) {
            throw new IllegalArgumentException(
                    "jastodon,owner.github-id must be set to a positive GitHub user ID");
        }
    }
}
