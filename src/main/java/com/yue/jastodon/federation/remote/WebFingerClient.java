package com.yue.jastodon.federation.remote;

import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class WebFingerClient {

    private final ActivityPubClient activityPubClient;

    WebFingerClient(ActivityPubClient activityPubClient) {
        this.activityPubClient = activityPubClient;
    }

    public URI resolve(Acct acct) {
        return null;
    }
}
