package com.yue.jastodon.federation.actor;

import com.yue.jastodon.AbstractIntegrationTest;
import com.yue.jastodon.federation.profile.Avatars;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AvatarServiceTest extends AbstractIntegrationTest {

    private static final byte[] IMAGE = {1, 2, 3};

    @Autowired
    AvatarService avatarService;

    @Autowired
    JdbcClient jdbcClient;

    @MockitoSpyBean
    Avatars avatars;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM profiles").update();
        jdbcClient.sql("DELETE FROM followers").update();
        jdbcClient.sql("DELETE FROM deliveries").update();
        doReturn("avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png").when(avatars).upload(any());
    }

    @Test
    void changeMakesTheUploadedAvatarCurrent() {
        URI url = avatarService.change(IMAGE);

        assertThat(url).isEqualTo(
                URI.create("https://media.jastodon.test/avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png"));
        assertThat(jdbcClient.sql("SELECT avatar_key FROM profiles WHERE username = 'yue'")
                .query(String.class)
                .single())
                .isEqualTo("avatars/9b2c7f4e-1d3a-4c8b-a6e5-0f1d2c3b4a59.png");
        verify(avatars).upload(IMAGE);
    }
}
