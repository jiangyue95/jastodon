package com.yue.jastodon.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    OwnerOnlyOAuth2UserService ownerOnlyOAuth2UserService(OwnerProperties ownerProperties) {
        return new OwnerOnlyOAuth2UserService(new DefaultOAuth2UserService(), ownerProperties);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, OwnerOnlyOAuth2UserService ownerOnlyOAuth2UserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(ownerOnlyOAuth2UserService)));
        return http.build();
    }
}
