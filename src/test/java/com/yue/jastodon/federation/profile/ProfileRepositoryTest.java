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
    private static final String NEW_AVATAR_KEY = "avatars/9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d.png";
    private static final String ALICE = "alice";

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

    @Test
    void replacesTheAvatarKey() {
        profileRepository.saveAvatarKey(YUE, AVATAR_KEY);

        profileRepository.saveAvatarKey(YUE, NEW_AVATAR_KEY);

        assertThat(profileRepository.findAvatarKey(YUE)).hasValue(NEW_AVATAR_KEY);
    }

    @Test
    void findsNothingForAnUnknownUsername() {
        profileRepository.saveAvatarKey(ALICE, AVATAR_KEY);

        assertThat(profileRepository.findAvatarKey(YUE)).isEmpty();
    }
}
