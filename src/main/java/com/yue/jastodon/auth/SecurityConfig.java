package com.yue.jastodon.auth;

import com.yue.jastodon.federation.FederationProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Order(1)
    @Bean
    SecurityFilterChain federationSecurityFilterChain(
            HttpSecurity http, FederationProperties federationProperties) throws Exception {
        http.securityMatcher("/.well-known/webfinger", federationProperties.actorId().getPath())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    OwnerOnlyOAuth2UserService ownerOnlyOAuth2UserService(OwnerProperties ownerProperties) {
        return new OwnerOnlyOAuth2UserService(new DefaultOAuth2UserService(), ownerProperties);
    }

    @Bean
    SecurityFilterChain browserSecurityFilterChain(
            HttpSecurity http, OwnerOnlyOAuth2UserService ownerOnlyOAuth2UserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(ownerOnlyOAuth2UserService)));
        return http.build();
    }
}
