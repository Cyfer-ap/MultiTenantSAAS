package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.NotificationDeliveryProperties;
import com.chacha.multitenantsaas.dto.NotificationDeliveryTask;
import com.chacha.multitenantsaas.entity.Notification;
import com.chacha.multitenantsaas.entity.NotificationDelivery;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationDeliveryStatus;
import com.chacha.multitenantsaas.repository.NotificationDeliveryRepository;
import com.chacha.multitenantsaas.repository.NotificationRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryService {

    private final NotificationDeliveryRepository repository;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryProperties properties;

    public NotificationDeliveryService(
            NotificationDeliveryRepository repository,
            NotificationRepository notificationRepository,
            NotificationDeliveryProperties properties) {
        this.repository = repository;
        this.notificationRepository = notificationRepository;
        this.properties = properties;
    }

    @Transactional
    public NotificationDelivery enqueue(
            Notification notification, NotificationDeliveryChannel channel) {
        Objects.requireNonNull(notification, "Notification is required");
        Objects.requireNonNull(channel, "Notification delivery channel is required");
        if (notification.getId() == null || notification.getTenant() == null) {
            throw new IllegalArgumentException("Notification must be persisted before delivery");
        }
        Notification lockedNotification =
                notificationRepository
                        .findByIdForUpdate(notification.getId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Notification must be persisted before delivery"));
        return repository
                .findByNotification_IdAndChannel(lockedNotification.getId(), channel)
                .orElseGet(
                        () ->
                                repository.save(
                                        new NotificationDelivery(
                                                lockedNotification.getTenant(),
                                                lockedNotification,
                                                channel,
                                                Instant.now())));
    }

    @Transactional
    public List<NotificationDeliveryTask> claimBatch(Instant now) {
        Instant staleBefore = now.minus(properties.getProcessingTimeout());
        List<NotificationDelivery> deliveries =
                repository.findClaimableForUpdate(
                        EnumSet.of(
                                NotificationDeliveryStatus.PENDING,
                                NotificationDeliveryStatus.RETRY),
                        NotificationDeliveryStatus.PROCESSING,
                        now,
                        staleBefore,
                        PageRequest.of(0, properties.getBatchSize()));

        return deliveries.stream()
                .filter(delivery -> prepareClaim(delivery, now))
                .map(this::toTask)
                .toList();
    }

    @Transactional
    public boolean markSent(UUID deliveryId, UUID leaseToken, Instant now) {
        return repository
                .findByIdForUpdate(deliveryId)
                .map(delivery -> delivery.markSent(leaseToken, now))
                .orElse(false);
    }

    @Transactional
    public boolean markFailed(UUID deliveryId, UUID leaseToken, Instant now, String error) {
        return repository
                .findByIdForUpdate(deliveryId)
                .map(
                        delivery ->
                                delivery.markFailedAttempt(
                                        leaseToken,
                                        now,
                                        error,
                                        properties.getMaxAttempts(),
                                        properties.getRetryBaseDelay(),
                                        properties.getRetryMaxDelay()))
                .orElse(false);
    }

    private boolean prepareClaim(NotificationDelivery delivery, Instant now) {
        if (delivery.getAttemptCount() >= properties.getMaxAttempts()) {
            delivery.failExpiredFinalLease(now);
            return false;
        }
        delivery.claim(now);
        return true;
    }

    private NotificationDeliveryTask toTask(NotificationDelivery delivery) {
        Notification notification = delivery.getNotification();
        return new NotificationDeliveryTask(
                delivery.getId(),
                delivery.getLeaseToken(),
                delivery.getTenant().getId(),
                notification.getId(),
                notification.getRecipientUser().getId(),
                notification.getRecipientUser().getEmail(),
                delivery.getChannel(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getTargetUrl());
    }
}
