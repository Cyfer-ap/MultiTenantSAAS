package com.chacha.multitenantsaas.service;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import com.chacha.multitenantsaas.entity.Tenant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMembershipNotificationServiceTest {

    @Mock private NotificationService notificationService;
    @Mock private Tenant tenant;
    @Mock private Project project;
    @Mock private AppUser actor;
    @Mock private AppUser member;

    private ProjectMembershipNotificationService service;
    private UUID projectId;
    private UUID actorId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        service = new ProjectMembershipNotificationService(notificationService);
        projectId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        lenient().when(project.getTenant()).thenReturn(tenant);
        lenient().when(project.getId()).thenReturn(projectId);
        lenient().when(project.getName()).thenReturn("Launch Project");
        lenient().when(actor.getId()).thenReturn(actorId);
        lenient().when(actor.getFullName()).thenReturn("Ada Admin");
        lenient().when(member.getId()).thenReturn(memberId);
    }

    @Test
    void notifiesMemberWhenAddedToProject() {
        service.notifyAdded(project, actor, member, ProjectMemberRole.MEMBER);

        verify(notificationService)
                .create(
                        tenant,
                        member,
                        NotificationType.PROJECT_MEMBERSHIP_CHANGED,
                        "Added to a project",
                        "Ada Admin added you to \"Launch Project\" as member.",
                        "/projects/" + projectId,
                        Set.of(NotificationDeliveryChannel.EMAIL));
    }

    @Test
    void notifiesMemberWhenProjectRoleChanges() {
        service.notifyRoleChanged(
                project, actor, member, ProjectMemberRole.MEMBER, ProjectMemberRole.PROJECT_LEAD);

        verify(notificationService)
                .create(
                        tenant,
                        member,
                        NotificationType.PROJECT_MEMBERSHIP_CHANGED,
                        "Project role changed",
                        "Ada Admin changed your role in \"Launch Project\" from member to project lead.",
                        "/projects/" + projectId,
                        Set.of(NotificationDeliveryChannel.EMAIL));
    }

    @Test
    void removalNotificationLinksToAccessibleProjectList() {
        service.notifyRemoved(project, actor, member);

        verify(notificationService)
                .create(
                        tenant,
                        member,
                        NotificationType.PROJECT_MEMBERSHIP_CHANGED,
                        "Removed from a project",
                        "Ada Admin removed you from \"Launch Project\".",
                        "/projects",
                        Set.of(NotificationDeliveryChannel.EMAIL));
    }

    @Test
    void suppressesSelfNotificationsAndNoOpRoleChanges() {
        when(member.getId()).thenReturn(actorId);

        service.notifyAdded(project, actor, member, ProjectMemberRole.MEMBER);
        service.notifyRemoved(project, actor, member);
        service.notifyRoleChanged(
                project, actor, member, ProjectMemberRole.MEMBER, ProjectMemberRole.PROJECT_LEAD);

        verifyNoInteractions(notificationService);

        service.notifyRoleChanged(
                project, actor, member, ProjectMemberRole.MEMBER, ProjectMemberRole.MEMBER);

        verify(notificationService, never())
                .create(
                        tenant,
                        member,
                        NotificationType.PROJECT_MEMBERSHIP_CHANGED,
                        "Project role changed",
                        "Ada Admin changed your role in \"Launch Project\" from member to member.",
                        "/projects/" + projectId,
                        Set.of(NotificationDeliveryChannel.EMAIL));
    }
}
