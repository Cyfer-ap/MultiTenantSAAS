package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.NotificationDeliveryTask;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "app.notifications.delivery",
        name = "enabled",
        havingValue = "true")
public class NotificationDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryWorker.class);

    private final NotificationDeliveryService deliveryService;
    private final Map<NotificationDeliveryChannel, NotificationDeliveryHandler> handlers;

    public NotificationDeliveryWorker(
            NotificationDeliveryService deliveryService,
            List<NotificationDeliveryHandler> handlers) {
        this.deliveryService = deliveryService;
        this.handlers = new EnumMap<>(NotificationDeliveryChannel.class);
        for (NotificationDeliveryHandler handler : handlers) {
            if (this.handlers.put(handler.channel(), handler) != null) {
                throw new IllegalStateException(
                        "Multiple notification delivery handlers configured for "
                                + handler.channel());
            }
        }
    }

    @Scheduled(
            initialDelayString = "${app.notifications.delivery.initial-delay-ms:60000}",
            fixedDelayString = "${app.notifications.delivery.interval-ms:30000}")
    public void processBatch() {
        for (NotificationDeliveryTask task : deliveryService.claimBatch(Instant.now())) {
            process(task);
        }
    }

    private void process(NotificationDeliveryTask task) {
        try {
            NotificationDeliveryHandler handler = handlers.get(task.channel());
            if (handler == null) {
                throw new IllegalStateException(
                        "No notification delivery handler configured for " + task.channel());
            }
            handler.deliver(task);
            deliveryService.markSent(task.deliveryId(), task.leaseToken(), Instant.now());
        } catch (RuntimeException exception) {
            log.warn(
                    "Notification delivery {} failed and will follow retry policy",
                    task.deliveryId(),
                    exception);
            deliveryService.markFailed(
                    task.deliveryId(), task.leaseToken(), Instant.now(), exception.getMessage());
        }
    }
}
