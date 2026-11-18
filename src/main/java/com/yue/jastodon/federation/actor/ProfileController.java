package com.yue.jastodon.federation.actor;

import com.yue.jastodon.federation.FederationProperties;
import com.yue.jastodon.federation.profile.Avatars;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
class ProfileController {

    private final FederationProperties federationProperties;
    private final Avatars avatars;

    ProfileController(FederationProperties federationProperties, Avatars avatars) {
        this.federationProperties = federationProperties;
        this.avatars = avatars;
    }

    @GetMapping("/profile")
    ProfileResponse show() {
        URI avatarUrl = avatars.currentUrl(federationProperties.username()).orElse(null);
        return new ProfileResponse(avatarUrl);
    }

    record ProfileResponse(URI avatarUrl) {
    }
}
