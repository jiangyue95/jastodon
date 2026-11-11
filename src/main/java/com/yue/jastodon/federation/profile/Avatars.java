package com.yue.jastodon.federation.profile;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;

@Component
public class Avatars {

    private final ProfileRepository profileRepository;
    private final MediaProperties mediaProperties;

    Avatars(ProfileRepository profileRepository, MediaProperties mediaProperties) {
        this.profileRepository = profileRepository;
        this.mediaProperties = mediaProperties;
    }

    public Optional<URI> currentUrl(String username) {
        return profileRepository.findAvatarKey(username)
                .map(key -> URI.create(mediaProperties.baseUrl() + "/" + key));
    }
}
