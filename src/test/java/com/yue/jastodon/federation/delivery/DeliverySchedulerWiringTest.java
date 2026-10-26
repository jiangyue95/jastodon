package com.yue.jastodon.federation.delivery;

import com.yue.jastodon.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeliverySchedulerWiringTest extends AbstractIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void schedulerDoesNotRunInTests() {
        assertThat(context.getBeanProvider(DeliveryScheduler.class).getIfAvailable()).isNull();
    }

}
