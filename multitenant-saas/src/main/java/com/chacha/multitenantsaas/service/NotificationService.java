package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.NotificationResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Notification;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.NotificationRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_BODY_LENGTH = 1000;
    private static final int MAX_TARGET_URL_LENGTH = 1000;

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryService notificationDeliveryService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationDeliveryService notificationDeliveryService) {
        this.notificationRepository = notificationRepository;
        this.notificationDeliveryService = notificationDeliveryService;
    }

    @Transactional
    public NotificationResponse create(
            Tenant tenant,
            AppUser recipientUser,
            NotificationType type,
            String title,
            String body,
            String targetUrl) {
        return create(tenant, recipientUser, type, title, body, targetUrl, Set.of());
    }

    @Transactional
    public NotificationResponse create(
            Tenant tenant,
            AppUser recipientUser,
            NotificationType type,
            String title,
            String body,
            String targetUrl,
            Set<NotificationDeliveryChannel> deliveryChannels) {
        validateRecipientScope(tenant, recipientUser);

        Notification notification =
                new Notification(
                        tenant,
                        recipientUser,
                        Objects.requireNonNull(type, "Notification type is required"),
                        normalizeRequired(title, "Notification title", MAX_TITLE_LENGTH),
                        normalizeRequired(body, "Notification body", MAX_BODY_LENGTH),
                        normalizeTargetUrl(targetUrl));

        Notification saved = notificationRepository.save(notification);
        Set.copyOf(Objects.requireNonNull(deliveryChannels, "Delivery channels are required"))
                .forEach(channel -> notificationDeliveryService.enqueue(saved, channel));
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(
            UUID tenantId, UUID recipientUserId, Pageable pageable) {
        Page<Notification> notifications =
                notificationRepository.findByTenant_IdAndRecipientUser_IdOrderByCreatedAtDesc(
                        tenantId, recipientUserId, pageable);

        return new PageResponse<>(
                notifications.getContent().stream().map(this::mapToResponse).toList(),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages(),
                notifications.isFirst(),
                notifications.isLast());
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID tenantId, UUID recipientUserId) {
        return notificationRepository.countByTenant_IdAndRecipientUser_IdAndReadAtIsNull(
                tenantId, recipientUserId);
    }

    @Transactional
    public NotificationResponse markRead(UUID tenantId, UUID recipientUserId, UUID notificationId) {
        Notification notification =
                notificationRepository
                        .findByTenant_IdAndRecipientUser_IdAndId(
                                tenantId, recipientUserId, notificationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Notification not found with id: "
                                                        + notificationId));

        notification.markRead(Instant.now());
        return mapToResponse(notification);
    }

    private void validateRecipientScope(Tenant tenant, AppUser recipientUser) {
        Objects.requireNonNull(tenant, "Notification tenant is required");
        Objects.requireNonNull(recipientUser, "Notification recipient is required");

        if (tenant.getId() == null
                || recipientUser.getTenant() == null
                || !tenant.getId().equals(recipientUser.getTenant().getId())) {
            throw new IllegalArgumentException(
                    "Notification recipient must belong to the notification tenant");
        }
    }

    private String normalizeRequired(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private String normalizeTargetUrl(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return null;
        }

        String normalized = targetUrl.trim();
        if (!normalized.startsWith("/") || normalized.startsWith("//")) {
            throw new IllegalArgumentException(
                    "Notification target URL must be an application-relative path");
        }
        if (normalized.length() > MAX_TARGET_URL_LENGTH) {
            throw new IllegalArgumentException(
                    "Notification target URL must not exceed "
                            + MAX_TARGET_URL_LENGTH
                            + " characters");
        }
        return normalized;
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTenant().getId(),
                notification.getRecipientUser().getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getTargetUrl(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
