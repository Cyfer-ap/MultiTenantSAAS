package com.chacha.multitenantsaas.workflows;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WorkflowDtos {

    private WorkflowDtos() {}

    public record NodeRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String key,
            @NotNull WorkflowNodeType type,
            @NotNull WorkflowOperation operation,
            @NotNull @Size(max = 2) Map<String, String> configuration,
            @Min(-100000) @Max(100000) int x,
            @Min(-100000) @Max(100000) int y) {}

    public record EdgeRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String sourceKey,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String targetKey,
            @NotNull WorkflowEdgeBranch branch) {}

    public record UpsertRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @Size(max = 1000) String description,
            @NotNull @Size(min = 2, max = 50) List<@Valid NodeRequest> nodes,
            @NotNull @Size(min = 1, max = 100) List<@Valid EdgeRequest> edges) {}

    public record NodeResponse(
            UUID id,
            String key,
            WorkflowNodeType type,
            WorkflowOperation operation,
            Map<String, String> configuration,
            int x,
            int y) {}

    public record EdgeResponse(
            UUID id, String sourceKey, String targetKey, WorkflowEdgeBranch branch) {}

    public record Response(
            UUID id,
            UUID tenantId,
            UUID createdByUserId,
            String name,
            String description,
            WorkflowStatus status,
            int definitionVersion,
            List<NodeResponse> nodes,
            List<EdgeResponse> edges,
            Instant createdAt,
            Instant updatedAt) {}
}
