package com.chacha.multitenantsaas.projectrisk.dto;

import com.chacha.multitenantsaas.projectrisk.ProjectRiskSeverity;
import com.chacha.multitenantsaas.projectrisk.ProjectRiskSignalType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProjectRiskDtos {

    private ProjectRiskDtos() {}

    public record Summary(
            int totalTasks,
            int openTasks,
            int overdueTasks,
            int blockedTasks,
            int staleTasks,
            int unassignedCriticalTasks,
            int dependencyBottlenecks) {}

    public record Signal(
            ProjectRiskSignalType type,
            ProjectRiskSeverity severity,
            UUID taskId,
            String taskTitle,
            String explanation,
            int affectedTaskCount,
            List<UUID> relatedTaskIds,
            Instant dueAt,
            Instant updatedAt) {

        public Signal {
            relatedTaskIds = relatedTaskIds == null ? List.of() : List.copyOf(relatedTaskIds);
        }
    }

    public record Response(
            UUID projectId,
            Instant generatedAt,
            ProjectRiskSeverity riskLevel,
            int staleAfterDays,
            Summary summary,
            List<Signal> signals,
            List<String> limitations) {

        public Response {
            signals = List.copyOf(signals);
            limitations = List.copyOf(limitations);
        }
    }
}
