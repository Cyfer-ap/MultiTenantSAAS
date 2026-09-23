package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.approvals.ApprovalDecisionOutcome;
import com.chacha.multitenantsaas.approvals.ApprovalRequestStatus;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ExternalAccessDtos {

    private ExternalAccessDtos() {}

    public record CreateGrantRequest(
            @NotBlank @Size(max = 150) String guestName,
            @NotBlank @Email @Size(max = 150) String guestEmail,
            @NotNull Instant expiresAt,
            @NotEmpty @Size(max = 4) Set<ExternalAccessCapability> capabilities) {}

    public record GrantResponse(
            UUID id,
            UUID projectId,
            String guestName,
            String guestEmail,
            Set<ExternalAccessCapability> capabilities,
            ExternalAccessGrantState state,
            Instant expiresAt,
            Instant acceptedAt,
            Instant revokedAt,
            UUID createdByUserId,
            UUID revokedByUserId,
            Instant createdAt,
            Instant updatedAt) {}

    public record GrantCreatedResponse(GrantResponse grant, String invitationToken) {}

    public record ExchangeRequest(@NotBlank @Size(max = 256) String invitationToken) {}

    public record ExchangeResponse(
            UUID grantId,
            UUID projectId,
            String guestName,
            String sessionToken,
            Instant sessionExpiresAt) {}

    public record ProjectResponse(
            UUID id, String name, String description, ProjectStatus status, Instant updatedAt) {}

    public record SessionResponse(
            UUID grantId,
            String guestName,
            String guestEmail,
            Set<ExternalAccessCapability> capabilities,
            Instant grantExpiresAt,
            Instant sessionExpiresAt,
            ProjectResponse project) {}

    public record TaskResponse(
            UUID id,
            String title,
            String description,
            ProjectTaskStatus status,
            ProjectTaskPriority priority,
            Instant dueAt,
            Instant completedAt,
            Instant updatedAt) {}

    public record TasksResponse(List<TaskResponse> tasks) {}

    public record GuestCommentRequest(@NotBlank @Size(max = 4000) String body) {}

    public record GuestCommentResponse(
            UUID id,
            UUID taskId,
            UUID grantId,
            String guestName,
            String guestEmail,
            String body,
            Instant createdAt) {}

    public record GuestCommentsResponse(List<GuestCommentResponse> comments) {}

    public record GuestApprovalReviewResponse(
            UUID requestId,
            UUID requestStageId,
            UUID taskId,
            String stageName,
            Instant createdAt) {}

    public record GuestApprovalReviewsResponse(List<GuestApprovalReviewResponse> reviews) {}

    public record GuestApprovalDecisionRequest(
            @NotNull ApprovalDecisionOutcome outcome, @Size(max = 1000) String comment) {}

    public record GuestApprovalDecisionResponse(
            UUID requestId,
            ApprovalRequestStatus status,
            int currentStageIndex,
            Instant completedAt) {}
}
