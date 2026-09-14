package com.chacha.multitenantsaas.tasktemplates;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class TaskTemplateDtos {

    private TaskTemplateDtos() {}

    public record UpsertRequest(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Size(min = 2, max = 200) String taskTitle,
            @Size(max = 4000) String taskDescription,
            @NotNull ProjectTaskPriority priority,
            UUID assigneeUserId,
            @Min(0) @Max(525600) Long dueOffsetMinutes) {}

    public record Response(
            UUID id,
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            UUID assigneeUserId,
            String name,
            String taskTitle,
            String taskDescription,
            ProjectTaskPriority priority,
            Long dueOffsetMinutes,
            Instant createdAt,
            Instant updatedAt) {}

    public record InstantiateResponse(UUID templateId, UUID taskId, Instant createdAt) {}
}
