package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.chacha.multitenantsaas.dto.NotificationResponse;
import com.chacha.multitenantsaas.email.EmailDeliveryException;
import com.chacha.multitenantsaas.email.EmailMessage;
import com.chacha.multitenantsaas.email.EmailSender;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationDelivery;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationDeliveryStatus;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.NotificationDeliveryRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.NotificationDeliveryWorker;
import com.chacha.multitenantsaas.service.NotificationService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties = {
            "app.notifications.delivery.enabled=true",
            "app.notifications.delivery.initial-delay-ms=3600000",
            "app.notifications.delivery.retry-base-delay=1s",
            "app.notifications.email.frontend-base-url=https://app.example.test"
        })
@ActiveProfiles("test")
@Transactional
class NotificationEmailDeliveryIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private NotificationDeliveryRepository deliveryRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationDeliveryWorker deliveryWorker;

    @MockitoBean private EmailSender emailSender;

    @Test
    void processesAnEmailOutboxRecordToSent() {
        NotificationResponse notification = createEmailNotification("email-success");

        deliveryWorker.processBatch();

        NotificationDelivery delivery = findDelivery(notification.id());
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(delivery.getSentAt()).isNotNull();
        verify(emailSender).send(any(EmailMessage.class));
    }

    @Test
    void recordsProviderFailureForRetryWithoutEscapingTheWorker() {
        doThrow(
                        new EmailDeliveryException(
                                "provider unavailable",
                                new IllegalStateException("connection failed")))
                .when(emailSender)
                .send(any(EmailMessage.class));
        NotificationResponse notification = createEmailNotification("email-retry");

        deliveryWorker.processBatch();

        NotificationDelivery delivery = findDelivery(notification.id());
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.RETRY);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getNextAttemptAt()).isNotNull();
        assertThat(delivery.getLastError()).isEqualTo("provider unavailable");
    }

    private NotificationResponse createEmailNotification(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Tenant tenant =
                tenantRepository.saveAndFlush(
                        new Tenant(prefix + " Tenant", prefix + "-" + suffix));
        AppUser recipient =
                appUserRepository.saveAndFlush(
                        new AppUser(
                                tenant,
                                "Email Recipient",
                                prefix + "." + suffix + "@example.test",
                                "password-hash",
                                UserRole.TENANT_USER));

        return notificationService.create(
                tenant,
                recipient,
                NotificationType.TASK_ASSIGNED,
                "Task assigned",
                "A task was assigned to you.",
                "/projects/project-1?task=task-1",
                Set.of(NotificationDeliveryChannel.EMAIL));
    }

    private NotificationDelivery findDelivery(UUID notificationId) {
        return deliveryRepository
                .findByNotification_IdAndChannel(notificationId, NotificationDeliveryChannel.EMAIL)
                .orElseThrow();
    }
}
