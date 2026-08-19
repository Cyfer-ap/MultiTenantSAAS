package com.chacha.multitenantsaas.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Notification;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.NotificationRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServicePreferenceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDeliveryService notificationDeliveryService;
    @Mock private NotificationPreferenceService notificationPreferenceService;
    @Mock private Tenant tenant;
    @Mock private AppUser recipient;

    private NotificationService service;
    private UUID tenantId;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        service =
                new NotificationService(
                        notificationRepository,
                        notificationDeliveryService,
                        notificationPreferenceService);
        tenantId = UUID.randomUUID();
        recipientId = UUID.randomUUID();

        when(tenant.getId()).thenReturn(tenantId);
        when(recipient.getId()).thenReturn(recipientId);
        when(recipient.getTenant()).thenReturn(tenant);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void persistsInAppNotificationButSkipsEmailEnqueueWhenRecipientOptedOut() {
        when(notificationPreferenceService.isEmailEnabled(
                        tenantId, recipientId, NotificationType.TASK_COMMENT_ADDED))
                .thenReturn(false);

        service.create(
                tenant,
                recipient,
                NotificationType.TASK_COMMENT_ADDED,
                "New comment",
                "A teammate commented on your task.",
                "/projects/example?task=task",
                Set.of(NotificationDeliveryChannel.EMAIL));

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationDeliveryService, never())
                .enqueue(any(Notification.class), any(NotificationDeliveryChannel.class));
    }

    @Test
    void enqueuesEmailWhenRecipientHasNotOptedOut() {
        when(notificationPreferenceService.isEmailEnabled(
                        tenantId, recipientId, NotificationType.TASK_ASSIGNED))
                .thenReturn(true);

        service.create(
                tenant,
                recipient,
                NotificationType.TASK_ASSIGNED,
                "Assigned",
                "You were assigned a task.",
                "/projects/example?task=task",
                Set.of(NotificationDeliveryChannel.EMAIL));

        verify(notificationDeliveryService)
                .enqueue(any(Notification.class), org.mockito.ArgumentMatchers.eq(NotificationDeliveryChannel.EMAIL));
    }
}
