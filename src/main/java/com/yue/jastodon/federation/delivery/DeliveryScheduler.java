package com.yue.jastodon.federation.delivery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@ConditionalOnBooleanProperty(name = "jastodon.delivery.enabled", matchIfMissing = true)
class DeliveryScheduler {

    private final DeliveryWorker deliveryWorker;

    DeliveryScheduler(DeliveryWorker deliveryWorker) {
        this.deliveryWorker = deliveryWorker;
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    void deliverDue() {
        deliveryWorker.deliverDue();
    }
}
