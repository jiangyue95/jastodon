package com.yue.jastodon.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class OwnerOnlyOAuth2UserServiceTest {

    private static final long OWNER_ID = 12345L;

    @Test
    void returnsUserWhenOwnerIdIsInteger() {
        OAuth2User user = githubUser(Map.of("id", 12345, "login", "owner"));
        OwnerOnlyOAuth2UserService service = serviceReturning(user);

        OAuth2User result = service.loadUser(null);

        assertThat(result).isSameAs(user);
    }

    @Test
    void returnsUserWhenOwnerIdIsLong() {
        OAuth2User user = githubUser(Map.of("id", 12345L, "login", "owner"));
        OwnerOnlyOAuth2UserService service = serviceReturning(user);

        OAuth2User result = service.loadUser(null);

        assertThat(result).isSameAs(user);
    }

    @Test
    void rejectsUserWithDifferentId() {
        OAuth2User user = githubUser(Map.of("id", 67890, "login", "stranger"));
        OwnerOnlyOAuth2UserService service = serviceReturning(user);

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void rejectsUserWithoutId() {
        OAuth2User user = githubUser(Map.of("login", "stranger"));
        OwnerOnlyOAuth2UserService service = serviceReturning(user);

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private static OwnerOnlyOAuth2UserService serviceReturning(OAuth2User user) {
        return new OwnerOnlyOAuth2UserService(request -> user, new OwnerProperties(OWNER_ID));
    }

    private static OAuth2User githubUser(Map<String, Object> attributes) {
        return new DefaultOAuth2User(List.of(), attributes, "login");
    }
}
