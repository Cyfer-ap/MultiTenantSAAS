package com.chacha.multitenantsaas.projectsimulation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProjectSimulationDtos {

    private ProjectSimulationDtos() {}

    public record Request(
            @Valid @Size(max = 100) List<TaskOverride> taskOverrides,
            @Valid @Size(max = 100) List<DependencyChange> dependencyChanges) {
        public Request {
            taskOverrides = taskOverrides == null ? List.of() : List.copyOf(taskOverrides);
            dependencyChanges =
                    dependencyChanges == null ? List.of() : List.copyOf(dependencyChanges);
        }
    }

    public record TaskOverride(
            @NotNull UUID taskId,
            Instant dueAt,
            boolean clearDueAt,
            UUID assigneeUserId,
            boolean clearAssignee) {}

    public record DependencyChange(
            @NotNull DependencyChangeType type,
            @NotNull UUID blockingTaskId,
            @NotNull UUID dependentTaskId) {}

    public enum DependencyChangeType {
        ADD,
        REMOVE
    }

    public enum ConflictChangeType {
        NEW,
        RESOLVED,
        UNCHANGED
    }

    public record BaselineResponse(
            UUID projectId,
            Instant generatedAt,
            List<BaselineTask> tasks,
            List<BaselineDependency> dependencies) {}

    public record BaselineTask(
            UUID taskId,
            String title,
            String status,
            UUID assigneeUserId,
            String assigneeName,
            Instant dueAt) {}

    public record BaselineDependency(UUID blockingTaskId, UUID dependentTaskId) {}

    public record Response(
            UUID projectId,
            Instant generatedAt,
            Summary summary,
            List<TaskImpact> taskImpacts,
            List<DependencyConflict> dependencyConflicts,
            List<WorkloadImpact> workloadImpacts,
            List<String> notes) {}

    public record Summary(
            int tasksEvaluated,
            int directTaskChanges,
            int dependencyChanges,
            int downstreamAffectedTasks,
            int baselineDependencyConflicts,
            int simulatedDependencyConflicts,
            int newDependencyConflicts,
            int resolvedDependencyConflicts,
            int reassignedTasks) {}

    public record TaskImpact(
            UUID taskId,
            String title,
            boolean directChange,
            boolean dependencyChanged,
            boolean downstreamAffected,
            Instant currentDueAt,
            Instant simulatedDueAt,
            Long dueDateShiftHours,
            UUID currentAssigneeUserId,
            String currentAssigneeName,
            UUID simulatedAssigneeUserId,
            String simulatedAssigneeName,
            boolean hasSimulatedDependencyConflict) {}

    public record DependencyConflict(
            UUID blockingTaskId,
            String blockingTaskTitle,
            UUID dependentTaskId,
            String dependentTaskTitle,
            ConflictChangeType changeType,
            Instant baselineBlockingDueAt,
            Instant baselineDependentDueAt,
            Instant simulatedBlockingDueAt,
            Instant simulatedDependentDueAt) {}

    public record WorkloadImpact(
            UUID userId,
            String displayName,
            int baselineOpenTasks,
            int simulatedOpenTasks,
            int delta) {}
}
