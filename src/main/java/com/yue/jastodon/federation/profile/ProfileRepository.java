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
        jdbcClient.sql("""
                       INSERT INTO profiles (username, avatar_key)
                       VALUES (:username, :avatarKey)
                       ON CONFLICT (username) DO UPDATE SET avatar_key = EXCLUDED.avatar_key
                       """)
                .param("username", username)
                .param("avatarKey", avatarKey)
                .update();
    }

    Optional<String> findAvatarKey(String username) {
        return jdbcClient.sql("""
                              SELECT avatar_key
                              FROM profiles
                              WHERE username = :username
                              """)
                .param("username", username)
                .query(String.class)
                .optional();
    }
}
