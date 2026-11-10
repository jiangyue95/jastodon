package com.yue.jastodon.federation.profile;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProfileRepositoryTest extends AbstractIntegrationTest {

    private static final String YUE = "yue";
    private static final String AVATAR_KEY = "avatars/6f1c2a3e-8b4d-4e7a-9c5f-2d3e4f5a6b7c.png";

    @Autowired
    ProfileRepository profileRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void emptyTable() {
        jdbcClient.sql("DELETE FROM profiles").update();
    }

    @Test
    void savesTheAvatarKey() {
        profileRepository.saveAvatarKey(YUE, AVATAR_KEY);

        assertThat(profileRepository.findAvatarKey(YUE)).hasValue(AVATAR_KEY);
    }
}
