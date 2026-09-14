package com.chacha.multitenantsaas.recurringwork;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class RecurringTaskDtos {

    private RecurringTaskDtos() {}

    public record CreateRequest(
            @NotBlank @Size(min = 2, max = 200) String title,
            @Size(max = 4000) String description,
            @NotNull ProjectTaskPriority priority,
            UUID assigneeUserId,
            @NotNull RecurrenceCadence cadence,
            @Min(1) @Max(52) int intervalCount,
            @NotBlank @Size(max = 80) String zoneId,
            @NotNull Instant nextOccurrenceAt,
            @Min(0) @Max(525600) Long dueOffsetMinutes,
            Instant endAt,
            @Min(1) @Max(10000) Integer maxOccurrences) {}

    public record UpdateRequest(
            @NotBlank @Size(min = 2, max = 200) String title,
            @Size(max = 4000) String description,
            @NotNull ProjectTaskPriority priority,
            UUID assigneeUserId,
            @NotNull RecurrenceCadence cadence,
            @Min(1) @Max(52) int intervalCount,
            @NotBlank @Size(max = 80) String zoneId,
            @NotNull Instant nextOccurrenceAt,
            @Min(0) @Max(525600) Long dueOffsetMinutes,
            Instant endAt,
            @Min(1) @Max(10000) Integer maxOccurrences) {}

    public record Response(
            UUID id,
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            UUID assigneeUserId,
            String title,
            String description,
            ProjectTaskPriority priority,
            RecurrenceCadence cadence,
            int intervalCount,
            String zoneId,
            Instant nextOccurrenceAt,
            Long dueOffsetMinutes,
            Instant endAt,
            Integer maxOccurrences,
            int generatedCount,
            RecurrenceStatus status,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {}

    public record OccurrenceResponse(
            UUID id, Instant scheduledFor, UUID taskId, Instant createdAt) {}
}
