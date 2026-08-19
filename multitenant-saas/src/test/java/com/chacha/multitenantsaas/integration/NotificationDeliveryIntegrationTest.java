package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.dto.NotificationDeliveryTask;
import com.chacha.multitenantsaas.dto.NotificationResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Notification;
import com.chacha.multitenantsaas.entity.NotificationDelivery;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationDeliveryStatus;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.NotificationDeliveryRepository;
import com.chacha.multitenantsaas.repository.NotificationRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.NotificationDeliveryService;
import com.chacha.multitenantsaas.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties = {
            "app.notifications.delivery.max-attempts=3",
            "app.notifications.delivery.processing-timeout=5s",
            "app.notifications.delivery.retry-base-delay=1s",
            "app.notifications.delivery.retry-max-delay=4s"
        })
@ActiveProfiles("test")
@Transactional
class NotificationDeliveryIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationDeliveryRepository deliveryRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationDeliveryService deliveryService;

    @BeforeEach
    void clearDeliveryOutbox() {
        deliveryRepository.deleteAll();
        deliveryRepository.flush();
    }

    @Test
    void createsNotificationAndEmailOutboxAtomicallyAndIdempotently() {
        Tenant tenant = createTenant("delivery-atomic");
        AppUser recipient = createUser(tenant, "atomic");

        NotificationResponse response = createEmailNotification(tenant, recipient);
        Notification notification = notificationRepository.findById(response.id()).orElseThrow();
        NotificationDelivery delivery =
                deliveryRepository
                        .findByNotification_IdAndChannel(
                                notification.getId(), NotificationDeliveryChannel.EMAIL)
                        .orElseThrow();

        assertThat(delivery.getTenant().getId()).isEqualTo(tenant.getId());
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(delivery.getAttemptCount()).isZero();

        NotificationDelivery duplicate =
                deliveryService.enqueue(notification, NotificationDeliveryChannel.EMAIL);
        assertThat(duplicate.getId()).isEqualTo(delivery.getId());
        assertThat(deliveryRepository.count()).isEqualTo(1L);
    }

    @Test
    void retriesWithBackoffAndRejectsStaleWorkerCompletion() {
        Tenant tenant = createTenant("delivery-retry");
        AppUser recipient = createUser(tenant, "retry");
        createEmailNotification(tenant, recipient);

        Instant firstAttempt = Instant.now().plusSeconds(1);
        NotificationDeliveryTask first = onlyClaim(firstAttempt);

        assertThat(deliveryService.claimBatch(firstAttempt)).isEmpty();
        assertThat(
                        deliveryService.markFailed(
                                first.deliveryId(),
                                UUID.randomUUID(),
                                firstAttempt,
                                "wrong worker"))
                .isFalse();
        assertThat(
                        deliveryService.markFailed(
                                first.deliveryId(),
                                first.leaseToken(),
                                firstAttempt,
                                "provider unavailable"))
                .isTrue();

        NotificationDelivery retrying =
                deliveryRepository.findById(first.deliveryId()).orElseThrow();
        assertThat(retrying.getStatus()).isEqualTo(NotificationDeliveryStatus.RETRY);
        assertThat(retrying.getAttemptCount()).isEqualTo(1);
        Instant expectedRetry = firstAttempt.plusSeconds(1);
        assertThat(retrying.getNextAttemptAt())
                .isBetween(expectedRetry.minusNanos(1000), expectedRetry.plusNanos(1000));
        assertThat(deliveryService.claimBatch(firstAttempt.plusMillis(999))).isEmpty();

        NotificationDeliveryTask second = onlyClaim(expectedRetry.plusMillis(1));
        assertThat(second.leaseToken()).isNotEqualTo(first.leaseToken());
        assertThat(
                        deliveryService.markSent(
                                second.deliveryId(),
                                first.leaseToken(),
                                firstAttempt.plusSeconds(2)))
                .isFalse();
        assertThat(
                        deliveryService.markSent(
                                second.deliveryId(),
                                second.leaseToken(),
                                firstAttempt.plusSeconds(2)))
                .isTrue();

        NotificationDelivery sent = deliveryRepository.findById(first.deliveryId()).orElseThrow();
        assertThat(sent.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(sent.getAttemptCount()).isEqualTo(2);
        assertThat(sent.getSentAt()).isEqualTo(firstAttempt.plusSeconds(2));
    }

    @Test
    void recoversExpiredLeaseAndDatabaseRejectsCrossTenantNotificationReference() {
        Tenant firstTenant = createTenant("delivery-lease-first");
        AppUser recipient = createUser(firstTenant, "lease");
        NotificationResponse response = createEmailNotification(firstTenant, recipient);

        Instant firstAttempt = Instant.now().plusSeconds(1);
        NotificationDeliveryTask abandoned = onlyClaim(firstAttempt);
        NotificationDeliveryTask recovered = onlyClaim(firstAttempt.plusSeconds(6));

        assertThat(recovered.deliveryId()).isEqualTo(abandoned.deliveryId());
        assertThat(recovered.leaseToken()).isNotEqualTo(abandoned.leaseToken());
        assertThat(
                        deliveryService.markSent(
                                abandoned.deliveryId(),
                                abandoned.leaseToken(),
                                firstAttempt.plusSeconds(7)))
                .isFalse();

        Tenant secondTenant = createTenant("delivery-lease-second");
        Notification notification = notificationRepository.findById(response.id()).orElseThrow();
        NotificationDelivery invalid =
                new NotificationDelivery(
                        secondTenant,
                        notification,
                        NotificationDeliveryChannel.EMAIL,
                        Instant.now());

        assertThatThrownBy(() -> deliveryRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private NotificationDeliveryTask onlyClaim(Instant now) {
        List<NotificationDeliveryTask> claimed = deliveryService.claimBatch(now);
        assertThat(claimed).hasSize(1);
        return claimed.getFirst();
    }

    private NotificationResponse createEmailNotification(Tenant tenant, AppUser recipient) {
        return notificationService.create(
                tenant,
                recipient,
                NotificationType.TASK_ASSIGNED,
                "Task assigned",
                "A task was assigned to you.",
                "/projects/project-1?task=task-1",
                Set.of(NotificationDeliveryChannel.EMAIL));
    }

    private Tenant createTenant(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return tenantRepository.saveAndFlush(new Tenant(prefix + " Tenant", prefix + "-" + suffix));
    }

    private AppUser createUser(Tenant tenant, String prefix) {
        return appUserRepository.saveAndFlush(
                new AppUser(
                        tenant,
                        prefix + " Recipient",
                        prefix + "." + UUID.randomUUID() + "@example.test",
                        "password-hash",
                        UserRole.TENANT_USER));
    }
}
