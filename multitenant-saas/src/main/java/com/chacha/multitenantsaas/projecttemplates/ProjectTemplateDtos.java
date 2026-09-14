package com.chacha.multitenantsaas.projecttemplates;

import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProjectTemplateDtos {

    private ProjectTemplateDtos() {}

    public record TaskSnapshotRequest(
            @NotBlank @Size(min = 2, max = 200) String title,
            @Size(max = 4000) String description,
            @NotNull ProjectTaskPriority priority,
            @Min(0) @Max(525600) Long dueOffsetMinutes) {}

    public record UpsertRequest(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Size(min = 2, max = 150) String projectNameSeed,
            @Size(max = 2000) String projectDescription,
            @NotNull ProjectStatus initialStatus,
            @NotNull @Size(max = 50) List<@Valid TaskSnapshotRequest> tasks) {}

    public record TaskSnapshotResponse(
            UUID id,
            int position,
            String title,
            String description,
            ProjectTaskPriority priority,
            Long dueOffsetMinutes) {}

    public record Response(
            UUID id,
            UUID tenantId,
            UUID createdByUserId,
            String name,
            String projectNameSeed,
            String projectDescription,
            ProjectStatus initialStatus,
            List<TaskSnapshotResponse> tasks,
            Instant createdAt,
            Instant updatedAt) {}

    public record InstantiateRequest(@Size(min = 2, max = 150) String projectName) {}

    public record InstantiateResponse(
            UUID templateId, UUID projectId, int tasksCreated, Instant createdAt) {}
}
