package com.yue.jastodon.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OwnerOnlyOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    private final OwnerProperties ownerProperties;

    public OwnerOnlyOAuth2UserService(OwnerProperties ownerProperties) {
        this.ownerProperties = ownerProperties;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(userRequest);
        if (!isOwner(user)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied"),
                    "Only the owner may sign in");
        }
        return user;
    }

    private boolean isOwner(OAuth2User user) {
        Number id = user.getAttribute("id");
        return id != null && id.longValue() == ownerProperties.githubId();
    }
}
