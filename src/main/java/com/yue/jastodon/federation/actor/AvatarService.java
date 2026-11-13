package com.yue.jastodon.federation.actor;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.profile.Avatars;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
class AvatarService {

    private final Avatars avatars;
    private final FederationProperties federationProperties;

    AvatarService(Avatars avatars, FederationProperties federationProperties) {
        this.avatars = avatars;
        this.federationProperties = federationProperties;
    }

    URI change(byte[] image) {
        String key = avatars.upload(image);
        String username = federationProperties.username();
        avatars.makeCurrent(username, key);
        return avatars.currentUrl(username).orElseThrow();
    }
}
