package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.OutboundWebhookDeliveryProperties;
import com.chacha.multitenantsaas.dto.OutboundWebhookDeliveryTask;
import com.chacha.multitenantsaas.entity.OutboundWebhookDelivery;
import com.chacha.multitenantsaas.entity.OutboundWebhookDeliveryStatus;
import com.chacha.multitenantsaas.entity.OutboundWebhookEndpoint;
import com.chacha.multitenantsaas.entity.OutboundWebhookEvent;
import com.chacha.multitenantsaas.repository.OutboundWebhookDeliveryRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundWebhookDeliveryService {

    private final OutboundWebhookDeliveryRepository repository;
    private final OutboundWebhookDeliveryProperties properties;

    public OutboundWebhookDeliveryService(
            OutboundWebhookDeliveryRepository repository,
            OutboundWebhookDeliveryProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public List<OutboundWebhookDeliveryTask> claimBatch(Instant now) {
        Instant staleBefore = now.minus(properties.getProcessingTimeout());
        List<OutboundWebhookDelivery> deliveries =
                repository.findClaimableForUpdate(
                        EnumSet.of(
                                OutboundWebhookDeliveryStatus.PENDING,
                                OutboundWebhookDeliveryStatus.RETRY),
                        OutboundWebhookDeliveryStatus.PROCESSING,
                        now,
                        staleBefore,
                        PageRequest.of(0, properties.getBatchSize()));

        return deliveries.stream()
                .filter(delivery -> prepareClaim(delivery, now))
                .map(this::toTask)
                .toList();
    }

    @Transactional
    public boolean markSent(UUID deliveryId, UUID leaseToken, Instant now, int httpStatus) {
        return repository
                .findByIdForUpdate(deliveryId)
                .map(delivery -> delivery.markSent(leaseToken, now, httpStatus))
                .orElse(false);
    }

    @Transactional
    public boolean markFailed(
            UUID deliveryId,
            UUID leaseToken,
            Instant now,
            Integer httpStatus,
            String error) {
        return repository
                .findByIdForUpdate(deliveryId)
                .map(
                        delivery ->
                                delivery.markFailedAttempt(
                                        leaseToken,
                                        now,
                                        httpStatus,
                                        error,
                                        properties.getMaxAttempts(),
                                        properties.getRetryBaseDelay(),
                                        properties.getRetryMaxDelay()))
                .orElse(false);
    }

    private boolean prepareClaim(OutboundWebhookDelivery delivery, Instant now) {
        if (delivery.getAttemptCount() >= properties.getMaxAttempts()) {
            delivery.failExpiredFinalLease(now);
            return false;
        }
        delivery.claim(now);
        return true;
    }

    private OutboundWebhookDeliveryTask toTask(OutboundWebhookDelivery delivery) {
        OutboundWebhookEndpoint endpoint = delivery.getEndpoint();
        OutboundWebhookEvent event = delivery.getEvent();
        return new OutboundWebhookDeliveryTask(
                delivery.getId(),
                delivery.getLeaseToken(),
                delivery.getTenant().getId(),
                endpoint.getId(),
                event.getId(),
                event.getEventType(),
                endpoint.getUrl(),
                endpoint.getSecretCiphertext(),
                endpoint.getSecretVersion(),
                event.getPayloadJson(),
                event.getOccurredAt());
    }
}
