package com.chacha.multitenantsaas.externalaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.taskcollaboration.external.ExternalTaskCommentCommand;
import com.chacha.multitenantsaas.taskcollaboration.external.ExternalTaskCommentPort;
import com.chacha.multitenantsaas.taskcollaboration.external.ExternalTaskCommentSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalPortalServiceTest {

    @Mock private ExternalGuestSessionService sessionService;
    @Mock private ExternalProjectProjectionPort projectProjectionPort;
    @Mock private ExternalTaskProjectionPort taskProjectionPort;
    @Mock private ExternalTaskCommentPort taskCommentPort;

    private ExternalPortalService service;

    @BeforeEach
    void setUp() {
        service =
                new ExternalPortalService(
                        sessionService,
                        projectProjectionPort,
                        taskProjectionPort,
                        taskCommentPort,
                        new ExternalAccessProperties());
    }

    @Test
    void taskReadUsesOnlyGrantBoundTenantAndProject() {
        UUID grantId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(3600);
        ExternalGuestSessionContext context =
                new ExternalGuestSessionContext(
                        grantId,
                        tenantId,
                        projectId,
                        "Client",
                        "client@example.com",
                        Set.of(
                                ExternalAccessCapability.PROJECT_READ,
                                ExternalAccessCapability.TASK_READ),
                        expiry,
                        expiry);
        when(sessionService.requireSession("session")).thenReturn(context);
        when(taskProjectionPort.listTasks(tenantId, projectId, 100))
                .thenReturn(
                        List.of(
                                new ExternalTaskSnapshot(
                                        UUID.randomUUID(),
                                        "Shared task",
                                        "Visible description",
                                        ProjectTaskStatus.TODO,
                                        ProjectTaskPriority.HIGH,
                                        null,
                                        null,
                                        Instant.now())));

        ExternalAccessDtos.TasksResponse response = service.tasks("session");

        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().getFirst().title()).isEqualTo("Shared task");
        verify(taskProjectionPort).listTasks(tenantId, projectId, 100);
    }

    @Test
    void guestCommentUsesOnlyGrantBoundScope() {
        UUID grantId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(3600);
        ExternalGuestSessionContext context =
                new ExternalGuestSessionContext(
                        grantId,
                        tenantId,
                        projectId,
                        "Client",
                        "client@example.com",
                        Set.of(
                                ExternalAccessCapability.PROJECT_READ,
                                ExternalAccessCapability.TASK_READ,
                                ExternalAccessCapability.TASK_COMMENT_CREATE),
                        expiry,
                        expiry);
        when(sessionService.requireSession("session")).thenReturn(context);
        when(taskCommentPort.createComment(
                        new ExternalTaskCommentCommand(
                                tenantId,
                                projectId,
                                taskId,
                                grantId,
                                "Client",
                                "client@example.com",
                                "Please review this.")))
                .thenReturn(
                        new ExternalTaskCommentSnapshot(
                                UUID.randomUUID(),
                                taskId,
                                grantId,
                                "Client",
                                "client@example.com",
                                "Please review this.",
                                Instant.now()));

        ExternalAccessDtos.GuestCommentResponse response =
                service.createComment(
                        "session",
                        taskId,
                        new ExternalAccessDtos.GuestCommentRequest("Please review this."));

        assertThat(response.taskId()).isEqualTo(taskId);
        assertThat(response.grantId()).isEqualTo(grantId);
        verify(taskCommentPort)
                .createComment(
                        new ExternalTaskCommentCommand(
                                tenantId,
                                projectId,
                                taskId,
                                grantId,
                                "Client",
                                "client@example.com",
                                "Please review this."));
    }

    @Test
    void missingCommentCapabilityStopsBeforeTaskCollaborationPort() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(3600);
        when(sessionService.requireSession("session"))
                .thenReturn(
                        new ExternalGuestSessionContext(
                                UUID.randomUUID(),
                                tenantId,
                                projectId,
                                "Client",
                                "client@example.com",
                                Set.of(
                                        ExternalAccessCapability.PROJECT_READ,
                                        ExternalAccessCapability.TASK_READ),
                                expiry,
                                expiry));

        assertThatThrownBy(
                        () ->
                                service.createComment(
                                        "session",
                                        taskId,
                                        new ExternalAccessDtos.GuestCommentRequest("Blocked")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not allow");

        verify(taskCommentPort, never()).createComment(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingTaskCapabilityStopsBeforeTaskDomainRead() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant expiry = Instant.now().plusSeconds(3600);
        when(sessionService.requireSession("session"))
                .thenReturn(
                        new ExternalGuestSessionContext(
                                UUID.randomUUID(),
                                tenantId,
                                projectId,
                                "Client",
                                "client@example.com",
                                Set.of(ExternalAccessCapability.PROJECT_READ),
                                expiry,
                                expiry));

        assertThatThrownBy(() -> service.tasks("session"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not allow");

        verify(taskProjectionPort, never()).listTasks(tenantId, projectId, 100);
    }
}
