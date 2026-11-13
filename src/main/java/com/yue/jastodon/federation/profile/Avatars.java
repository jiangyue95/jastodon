package com.yue.jastodon.federation.profile;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;

@Component
public class Avatars {

    private final ProfileRepository profileRepository;
    private final MediaProperties mediaProperties;
    private final AvatarStorage avatarStorage;

    Avatars(ProfileRepository profileRepository, MediaProperties mediaProperties, AvatarStorage avatarStorage) {
        this.profileRepository = profileRepository;
        this.mediaProperties = mediaProperties;
        this.avatarStorage = avatarStorage;
    }

    public Optional<URI> currentUrl(String username) {
        return profileRepository.findAvatarKey(username)
                .map(key -> URI.create(mediaProperties.baseUrl() + "/" + key));
    }

    public String upload(byte[] image) {
        byte[] png = AvatarImage.normalise(image);
        return avatarStorage.store(png);
    }

    public void makeCurrent(String username, String key) {
        profileRepository.saveAvatarKey(username, key);
    }
}
