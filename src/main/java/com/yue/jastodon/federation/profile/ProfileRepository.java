package com.yue.jastodon.federation.profile;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class ProfileRepository {

    private final JdbcClient jdbcClient;

    ProfileRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void saveAvatarKey(String username, String avatarKey) {
    }

    Optional<String> findAvatarKey(String username) {
        return Optional.empty();
    }
}
