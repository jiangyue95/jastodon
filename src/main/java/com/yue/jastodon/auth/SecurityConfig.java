package com.yue.jastodon.auth;

import com.yue.jastodon.federation.FederationProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
public class SecurityConfig {

    @Order(1)
    @Bean
    SecurityFilterChain federationSecurityFilterChain(
            HttpSecurity http, FederationProperties federationProperties) throws Exception {
        http.securityMatcher(
                "/.well-known/webfinger",
                federationProperties.actorId().getPath(),
                federationProperties.inbox().getPath(),
                federationProperties.outbox().getPath(),
                federationProperties.actorId().getPath() + "/statuses/*")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
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
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(ownerOnlyOAuth2UserService))
                        .defaultSuccessUrl("/compose/", true));
        return http.build();
    }

    private static AuthenticationEntryPoint authenticationEntryPoint() {
        return DelegatingAuthenticationEntryPoint.builder()
                .addEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new OrRequestMatcher(
                                PathPatternRequestMatcher.pathPattern("/csrf"),
                                PathPatternRequestMatcher.pathPattern("/notes")))
                .defaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github"))
                .build();
    }
}
