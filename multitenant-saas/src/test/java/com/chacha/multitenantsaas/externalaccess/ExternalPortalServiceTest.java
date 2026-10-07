package com.chacha.multitenantsaas.externalaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.approvals.ApprovalDecisionOutcome;
import com.chacha.multitenantsaas.approvals.ApprovalRequestStatus;
import com.chacha.multitenantsaas.approvals.ExternalApprovalDecisionCommand;
import com.chacha.multitenantsaas.approvals.ExternalApprovalDecisionResult;
import com.chacha.multitenantsaas.approvals.ExternalApprovalReviewPort;
import com.chacha.multitenantsaas.approvals.ExternalApprovalReviewSummary;
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
    @Mock private ExternalApprovalReviewPort approvalReviewPort;

    private ExternalPortalService service;

    @BeforeEach
    void setUp() {
        service =
                new ExternalPortalService(
                        sessionService,
                        projectProjectionPort,
                        taskProjectionPort,
                        taskCommentPort,
                        approvalReviewPort,
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
    void approvalReadUsesOnlyGrantBoundScope() {
        UUID grantId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
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
                                ExternalAccessCapability.APPROVAL_REVIEW),
                        expiry,
                        expiry);
        when(sessionService.requireSession("session")).thenReturn(context);
        when(approvalReviewPort.listPending(tenantId, projectId, grantId, 50))
                .thenReturn(
                        List.of(
                                new ExternalApprovalReviewSummary(
                                        requestId,
                                        stageId,
                                        taskId,
                                        "Client review",
                                        Instant.now())));

        ExternalAccessDtos.GuestApprovalReviewsResponse response = service.approvals("session");

        assertThat(response.reviews()).hasSize(1);
        assertThat(response.reviews().getFirst().requestId()).isEqualTo(requestId);
        assertThat(response.reviews().getFirst().requestStageId()).isEqualTo(stageId);
        verify(approvalReviewPort).listPending(tenantId, projectId, grantId, 50);
    }

    @Test
    void approvalDecisionUsesOnlyGrantBoundScope() {
        UUID grantId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
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
                                ExternalAccessCapability.APPROVAL_REVIEW),
                        expiry,
                        expiry);
        ExternalApprovalDecisionCommand command =
                new ExternalApprovalDecisionCommand(
                        tenantId,
                        projectId,
                        requestId,
                        grantId,
                        ApprovalDecisionOutcome.APPROVE,
                        "Looks good");
        when(sessionService.requireSession("session")).thenReturn(context);
        when(approvalReviewPort.decide(command))
                .thenReturn(
                        new ExternalApprovalDecisionResult(
                                requestId, ApprovalRequestStatus.APPROVED, 0, Instant.now()));

        ExternalAccessDtos.GuestApprovalDecisionResponse response =
                service.decideApproval(
                        "session",
                        requestId,
                        new ExternalAccessDtos.GuestApprovalDecisionRequest(
                                ApprovalDecisionOutcome.APPROVE, "Looks good"));

        assertThat(response.requestId()).isEqualTo(requestId);
        assertThat(response.status()).isEqualTo(ApprovalRequestStatus.APPROVED);
        verify(approvalReviewPort).decide(command);
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
    void missingApprovalCapabilityStopsBeforeApprovalPort() {
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
                                Set.of(
                                        ExternalAccessCapability.PROJECT_READ,
                                        ExternalAccessCapability.TASK_READ),
                                expiry,
                                expiry));

        assertThatThrownBy(() -> service.approvals("session"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not allow");

        verifyNoInteractions(approvalReviewPort);
    }

    @Test
    void revokedOrExpiredSessionStopsBeforeApprovalPort() {
        when(sessionService.requireSession("session"))
                .thenThrow(new IllegalArgumentException("Guest session is no longer active"));

        assertThatThrownBy(() -> service.approvals("session"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no longer active");

        verifyNoInteractions(approvalReviewPort);
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
