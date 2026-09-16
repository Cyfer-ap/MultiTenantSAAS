package com.chacha.multitenantsaas.approvals;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApprovalDtos {

    private ApprovalDtos() {}

    public record StageRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{0,63}$") String key,
            @NotBlank @Size(max = 120) String name,
            boolean allowRequesterApproval,
            @NotEmpty @Size(max = 20) List<@NotNull UUID> reviewerUserIds) {}

    public record UpsertRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @Size(max = 1000) String description,
            @NotEmpty @Size(max = 10) List<@Valid StageRequest> stages) {}

    public record StageResponse(
            UUID id,
            String key,
            String name,
            int position,
            boolean allowRequesterApproval,
            List<UUID> reviewerUserIds) {}

    public record DefinitionResponse(
            UUID id,
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            String name,
            String description,
            ApprovalStatus status,
            int definitionVersion,
            List<StageResponse> stages,
            Instant createdAt,
            Instant updatedAt) {}

    public record DefinitionSummary(
            UUID id,
            String name,
            ApprovalStatus status,
            int definitionVersion,
            int stageCount,
            Instant updatedAt) {}

    public record DecisionRequest(
            @NotNull ApprovalDecisionOutcome outcome,
            @Size(max = 1000) String comment) {}

    public record RequestStageResponse(
            UUID id,
            String key,
            String name,
            int position,
            boolean allowRequesterApproval,
            ApprovalStageStatus status,
            List<UUID> reviewerUserIds,
            UUID decidedByUserId,
            String decisionComment,
            Instant decidedAt) {}

    public record RequestResponse(
            UUID id,
            UUID projectId,
            UUID definitionId,
            int definitionVersion,
            UUID workflowId,
            int workflowVersion,
            UUID workflowExecutionId,
            String workflowNodeKey,
            UUID taskId,
            UUID actorUserId,
            ApprovalRequestStatus status,
            int currentStageIndex,
            List<RequestStageResponse> stages,
            Instant createdAt,
            Instant completedAt) {}

    public record RequestSummary(
            UUID id,
            UUID definitionId,
            UUID workflowId,
            UUID taskId,
            ApprovalRequestStatus status,
            int currentStageIndex,
            String currentStageName,
            Instant createdAt,
            Instant completedAt) {}
}
