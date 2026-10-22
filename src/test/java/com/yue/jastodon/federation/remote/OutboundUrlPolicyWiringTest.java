package com.yue.jastodon.federation.remote;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OutboundUrlPolicyWiringTest extends AbstractIntegrationTest {

    @Autowired
    OutboundUrlPolicy outboundUrlPolicy;

    @Test
    void productionPolicyRejectsCloudMetadataUrl() {
        assertThatThrownBy(() -> outboundUrlPolicy.check(URI.create("https://169.254.169.254/latest/meta-data/")))
                .isInstanceOf(UnsafeRemoteUrlException.class)
                .hasMessageContaining("non-public address");
    }

}
