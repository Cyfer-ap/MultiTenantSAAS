package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.OutboundWebhookDeliveryTask;
import com.chacha.multitenantsaas.exception.OutboundWebhookDeliveryException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "app.outbound-webhooks.delivery",
        name = "enabled",
        havingValue = "true")
public class OutboundWebhookDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboundWebhookDeliveryWorker.class);

    private final OutboundWebhookDeliveryService deliveryService;
    private final OutboundWebhookHttpTransport transport;

    public OutboundWebhookDeliveryWorker(
            OutboundWebhookDeliveryService deliveryService,
            OutboundWebhookHttpTransport transport) {
        this.deliveryService = deliveryService;
        this.transport = transport;
    }

    @Scheduled(
            initialDelayString = "${app.outbound-webhooks.delivery.initial-delay-ms:60000}",
            fixedDelayString = "${app.outbound-webhooks.delivery.interval-ms:30000}")
    public void processBatch() {
        for (OutboundWebhookDeliveryTask task : deliveryService.claimBatch(Instant.now())) {
            process(task);
        }
    }

    private void process(OutboundWebhookDeliveryTask task) {
        try {
            int status = transport.deliver(task);
            deliveryService.markSent(task.deliveryId(), task.leaseToken(), Instant.now(), status);
        } catch (OutboundWebhookDeliveryException exception) {
            log.warn(
                    "Outbound webhook delivery {} failed and will follow retry policy",
                    task.deliveryId(),
                    exception);
            deliveryService.markFailed(
                    task.deliveryId(),
                    task.leaseToken(),
                    Instant.now(),
                    exception.getHttpStatus(),
                    exception.getMessage());
        } catch (RuntimeException exception) {
            log.warn(
                    "Outbound webhook delivery {} failed unexpectedly and will follow retry policy",
                    task.deliveryId(),
                    exception);
            deliveryService.markFailed(
                    task.deliveryId(),
                    task.leaseToken(),
                    Instant.now(),
                    null,
                    exception.getMessage());
        }
    }
}
