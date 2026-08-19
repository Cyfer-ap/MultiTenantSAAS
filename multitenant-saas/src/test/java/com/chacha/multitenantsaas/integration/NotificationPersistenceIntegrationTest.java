package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.dto.NotificationResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.NotificationRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.NotificationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPersistenceIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;

    @Test
    void persistsAndReadsOnlyTheScopedRecipientsNotifications() {
        Tenant firstTenant = createTenant("notification-first");
        AppUser firstRecipient = createUser(firstTenant, "first");
        Tenant secondTenant = createTenant("notification-second");
        AppUser secondRecipient = createUser(secondTenant, "second");

        NotificationResponse created =
                notificationService.create(
                        firstTenant,
                        firstRecipient,
                        NotificationType.TASK_COMMENT_MENTIONED,
                        "  You were mentioned  ",
                        "  A teammate mentioned you in a task comment.  ",
                        "/projects/project-1?task=task-1&comment=comment-1");

        assertThat(created.id()).isNotNull();
        assertThat(created.tenantId()).isEqualTo(firstTenant.getId());
        assertThat(created.recipientUserId()).isEqualTo(firstRecipient.getId());
        assertThat(created.title()).isEqualTo("You were mentioned");
        assertThat(created.body()).isEqualTo("A teammate mentioned you in a task comment.");
        assertThat(created.readAt()).isNull();
        assertThat(created.createdAt()).isNotNull();

        PageResponse<NotificationResponse> firstRecipientNotifications =
                notificationService.getNotifications(
                        firstTenant.getId(), firstRecipient.getId(), PageRequest.of(0, 20));
        PageResponse<NotificationResponse> secondRecipientNotifications =
                notificationService.getNotifications(
                        secondTenant.getId(), secondRecipient.getId(), PageRequest.of(0, 20));

        assertThat(firstRecipientNotifications.content())
                .extracting(NotificationResponse::id)
                .containsExactly(created.id());
        assertThat(secondRecipientNotifications.content()).isEmpty();
        assertThat(notificationService.countUnread(firstTenant.getId(), firstRecipient.getId()))
                .isEqualTo(1L);
        assertThat(notificationService.countUnread(secondTenant.getId(), secondRecipient.getId()))
                .isZero();
    }

    @Test
    void rejectsCrossTenantRecipientsAndUnscopedReadMutations() {
        Tenant firstTenant = createTenant("notification-scope-first");
        AppUser firstRecipient = createUser(firstTenant, "scope-first");
        Tenant secondTenant = createTenant("notification-scope-second");
        AppUser secondRecipient = createUser(secondTenant, "scope-second");
        long existingNotificationCount = notificationRepository.count();

        assertThatThrownBy(
                        () ->
                                notificationService.create(
                                        firstTenant,
                                        secondRecipient,
                                        NotificationType.TASK_ASSIGNED,
                                        "Task assigned",
                                        "A task was assigned to you.",
                                        "/projects/project-1?task=task-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification recipient must belong to the notification tenant");
        assertThat(notificationRepository.count()).isEqualTo(existingNotificationCount);

        NotificationResponse created =
                notificationService.create(
                        firstTenant,
                        firstRecipient,
                        NotificationType.TASK_ASSIGNED,
                        "Task assigned",
                        "A task was assigned to you.",
                        "/projects/project-1?task=task-1");

        assertThatThrownBy(
                        () ->
                                notificationService.markRead(
                                        secondTenant.getId(),
                                        secondRecipient.getId(),
                                        created.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(notificationRepository.findById(created.id()))
                .get()
                .extracting("readAt")
                .isNull();

        NotificationResponse read =
                notificationService.markRead(
                        firstTenant.getId(), firstRecipient.getId(), created.id());

        assertThat(read.readAt()).isNotNull();
        assertThat(notificationService.countUnread(firstTenant.getId(), firstRecipient.getId()))
                .isZero();
    }

    @Test
    void rejectsExternalNotificationTargets() {
        Tenant tenant = createTenant("notification-target");
        AppUser recipient = createUser(tenant, "target");
        long existingNotificationCount = notificationRepository.count();

        assertThatThrownBy(
                        () ->
                                notificationService.create(
                                        tenant,
                                        recipient,
                                        NotificationType.SECURITY_ALERT,
                                        "Security alert",
                                        "Review your account activity.",
                                        "https://example.test/phishing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification target URL must be an application-relative path");
        assertThat(notificationRepository.count()).isEqualTo(existingNotificationCount);
    }

    @Test
    void marksEveryUnreadNotificationForOnlyTheScopedRecipient() {
        Tenant tenant = createTenant("notification-read-all");
        AppUser firstRecipient = createUser(tenant, "read-all-first");
        AppUser secondRecipient = createUser(tenant, "read-all-second");

        notificationService.create(
                tenant,
                firstRecipient,
                NotificationType.TASK_ASSIGNED,
                "First task",
                "A task was assigned to you.",
                "/projects/project-1?task=task-1");
        notificationService.create(
                tenant,
                firstRecipient,
                NotificationType.TASK_ASSIGNED,
                "Second task",
                "Another task was assigned to you.",
                "/projects/project-1?task=task-2");
        notificationService.create(
                tenant,
                secondRecipient,
                NotificationType.TASK_ASSIGNED,
                "Other recipient task",
                "This belongs to another recipient.",
                "/projects/project-1?task=task-3");

        int markedRead = notificationService.markAllRead(tenant.getId(), firstRecipient.getId());

        assertThat(markedRead).isEqualTo(2);
        assertThat(notificationService.countUnread(tenant.getId(), firstRecipient.getId()))
                .isZero();
        assertThat(notificationService.countUnread(tenant.getId(), secondRecipient.getId()))
                .isEqualTo(1);
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
