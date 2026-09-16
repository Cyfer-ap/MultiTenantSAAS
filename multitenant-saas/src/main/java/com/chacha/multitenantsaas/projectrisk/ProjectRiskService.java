package com.chacha.multitenantsaas.projectrisk;

import com.chacha.multitenantsaas.projectrisk.dto.ProjectRiskDtos.Response;
import com.chacha.multitenantsaas.projectrisk.dto.ProjectRiskDtos.Signal;
import com.chacha.multitenantsaas.projectrisk.dto.ProjectRiskDtos.Summary;
import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskDependencySource;
import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskDependencySource.DependencySnapshot;
import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskTaskSource;
import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskTaskSource.TaskSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectRiskService {

    public static final int MAX_TASKS = 500;
    public static final int MAX_DEPENDENCIES = 1_000;
    public static final int MAX_SIGNALS = 200;
    public static final int DEFAULT_STALE_AFTER_DAYS = 14;
    public static final int DEPENDENCY_BOTTLENECK_THRESHOLD = 3;

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_URGENT = "URGENT";

    private final ProjectRiskTaskSource taskSource;
    private final ProjectRiskDependencySource dependencySource;
    private final Clock clock;

    @Autowired
    public ProjectRiskService(
            ProjectRiskTaskSource taskSource, ProjectRiskDependencySource dependencySource) {
        this(taskSource, dependencySource, Clock.systemUTC());
    }

    ProjectRiskService(
            ProjectRiskTaskSource taskSource,
            ProjectRiskDependencySource dependencySource,
            Clock clock) {
        this.taskSource = taskSource;
        this.dependencySource = dependencySource;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Response analyze(UUID tenantId, UUID projectId) {
        Instant now = clock.instant();
        List<String> limitations = new ArrayList<>();

        List<TaskSnapshot> tasks = taskSource.findProjectTasks(tenantId, projectId, MAX_TASKS + 1);
        if (tasks.size() > MAX_TASKS) {
            tasks = List.copyOf(tasks.subList(0, MAX_TASKS));
            limitations.add(
                    "Risk analysis is bounded to the first "
                            + MAX_TASKS
                            + " project tasks; additional tasks were not analyzed.");
        }

        List<DependencySnapshot> dependencies =
                dependencySource.findProjectDependencies(tenantId, projectId, MAX_DEPENDENCIES + 1);
        if (dependencies.size() > MAX_DEPENDENCIES) {
            dependencies = List.copyOf(dependencies.subList(0, MAX_DEPENDENCIES));
            limitations.add(
                    "Dependency analysis is bounded to the first "
                            + MAX_DEPENDENCIES
                            + " project dependency edges; additional edges were not analyzed.");
        }

        Map<UUID, TaskSnapshot> taskById = new HashMap<>();
        for (TaskSnapshot task : tasks) {
            taskById.put(task.taskId(), task);
        }

        Map<UUID, Set<UUID>> outgoing = new HashMap<>();
        Map<UUID, Set<UUID>> unresolvedIncomingBlockers = new HashMap<>();
        for (DependencySnapshot dependency : dependencies) {
            TaskSnapshot blocker = taskById.get(dependency.blockingTaskId());
            TaskSnapshot dependent = taskById.get(dependency.dependentTaskId());
            if (blocker == null || dependent == null) {
                continue;
            }
            outgoing.computeIfAbsent(dependency.blockingTaskId(), ignored -> new LinkedHashSet<>())
                    .add(dependency.dependentTaskId());
            if (isOpen(blocker) && isOpen(dependent)) {
                unresolvedIncomingBlockers
                        .computeIfAbsent(
                                dependency.dependentTaskId(), ignored -> new LinkedHashSet<>())
                        .add(dependency.blockingTaskId());
            }
        }

        List<Signal> signals = new ArrayList<>();
        Set<UUID> overdueTaskIds = new HashSet<>();
        Set<UUID> blockedTaskIds = new HashSet<>();
        Set<UUID> staleTaskIds = new HashSet<>();
        Set<UUID> unassignedCriticalTaskIds = new HashSet<>();
        Set<UUID> dependencyBottleneckTaskIds = new HashSet<>();
        Instant staleCutoff = now.minus(DEFAULT_STALE_AFTER_DAYS, ChronoUnit.DAYS);

        for (TaskSnapshot task : tasks) {
            if (!isOpen(task)) {
                continue;
            }

            if (task.dueAt() != null && task.dueAt().isBefore(now)) {
                long overdueDays = Math.max(1, Duration.between(task.dueAt(), now).toDays());
                overdueTaskIds.add(task.taskId());
                signals.add(
                        signal(
                                ProjectRiskSignalType.OVERDUE_TASK,
                                overdueSeverity(overdueDays),
                                task,
                                "Task is overdue by approximately " + overdueDays + " day(s).",
                                1,
                                List.of()));
            }

            Set<UUID> blockers = unresolvedIncomingBlockers.getOrDefault(task.taskId(), Set.of());
            if (STATUS_BLOCKED.equals(task.status()) || !blockers.isEmpty()) {
                blockedTaskIds.add(task.taskId());
                String explanation =
                        !blockers.isEmpty()
                                ? "Task is blocked by "
                                        + blockers.size()
                                        + " unresolved dependency task(s)."
                                : "Task is explicitly marked BLOCKED.";
                signals.add(
                        signal(
                                ProjectRiskSignalType.BLOCKED_TASK,
                                ProjectRiskSeverity.HIGH,
                                task,
                                explanation,
                                Math.max(1, blockers.size()),
                                sortedIds(blockers)));
            }

            if (task.updatedAt() != null && task.updatedAt().isBefore(staleCutoff)) {
                long staleDays = Math.max(1, Duration.between(task.updatedAt(), now).toDays());
                staleTaskIds.add(task.taskId());
                signals.add(
                        signal(
                                ProjectRiskSignalType.STALE_TASK,
                                staleDays >= 30
                                        ? ProjectRiskSeverity.HIGH
                                        : ProjectRiskSeverity.MEDIUM,
                                task,
                                "Open task has not changed for approximately "
                                        + staleDays
                                        + " day(s).",
                                1,
                                List.of()));
            }

            if (task.assigneeUserId() == null && isCriticalPriority(task.priority())) {
                unassignedCriticalTaskIds.add(task.taskId());
                signals.add(
                        signal(
                                ProjectRiskSignalType.UNASSIGNED_CRITICAL_TASK,
                                PRIORITY_URGENT.equals(task.priority())
                                        ? ProjectRiskSeverity.HIGH
                                        : ProjectRiskSeverity.MEDIUM,
                                task,
                                "Open " + task.priority() + " priority task has no assignee.",
                                1,
                                List.of()));
            }
        }

        for (TaskSnapshot task : tasks) {
            if (!isOpen(task)) {
                continue;
            }
            List<UUID> affected = downstreamOpenTasks(task.taskId(), outgoing, taskById);
            if (affected.size() < DEPENDENCY_BOTTLENECK_THRESHOLD) {
                continue;
            }
            dependencyBottleneckTaskIds.add(task.taskId());
            signals.add(
                    signal(
                            ProjectRiskSignalType.DEPENDENCY_BOTTLENECK,
                            bottleneckSeverity(affected.size()),
                            task,
                            "This open task sits upstream of "
                                    + affected.size()
                                    + " open dependent task(s).",
                            affected.size(),
                            affected));
        }

        signals.sort(
                Comparator.comparingInt((Signal signal) -> signal.severity().ordinal())
                        .reversed()
                        .thenComparing(signal -> signal.type().name())
                        .thenComparing(
                                signal -> signal.taskTitle() == null ? "" : signal.taskTitle())
                        .thenComparing(signal -> signal.taskId().toString()));

        if (signals.size() > MAX_SIGNALS) {
            signals = List.copyOf(signals.subList(0, MAX_SIGNALS));
            limitations.add(
                    "The response is bounded to the "
                            + MAX_SIGNALS
                            + " highest-priority risk signals.");
        }

        limitations.add(
                "Workload pressure is not calculated yet because the current model has task assignments but no explicit capacity or availability contract; assignment counts are not treated as employee capacity.");

        int openTasks = (int) tasks.stream().filter(ProjectRiskService::isOpen).count();
        Summary summary =
                new Summary(
                        tasks.size(),
                        openTasks,
                        overdueTaskIds.size(),
                        blockedTaskIds.size(),
                        staleTaskIds.size(),
                        unassignedCriticalTaskIds.size(),
                        dependencyBottleneckTaskIds.size());

        ProjectRiskSeverity riskLevel =
                signals.stream()
                        .map(Signal::severity)
                        .max(Comparator.comparingInt(ProjectRiskSeverity::ordinal))
                        .orElse(ProjectRiskSeverity.NONE);

        return new Response(
                projectId, now, riskLevel, DEFAULT_STALE_AFTER_DAYS, summary, signals, limitations);
    }

    private Signal signal(
            ProjectRiskSignalType type,
            ProjectRiskSeverity severity,
            TaskSnapshot task,
            String explanation,
            int affectedTaskCount,
            List<UUID> relatedTaskIds) {
        return new Signal(
                type,
                severity,
                task.taskId(),
                task.title(),
                explanation,
                affectedTaskCount,
                relatedTaskIds,
                task.dueAt(),
                task.updatedAt());
    }

    private static boolean isOpen(TaskSnapshot task) {
        return !STATUS_COMPLETED.equals(task.status()) && !STATUS_CANCELLED.equals(task.status());
    }

    private static boolean isCriticalPriority(String priority) {
        return PRIORITY_HIGH.equals(priority) || PRIORITY_URGENT.equals(priority);
    }

    private static ProjectRiskSeverity overdueSeverity(long overdueDays) {
        if (overdueDays >= 14) {
            return ProjectRiskSeverity.CRITICAL;
        }
        if (overdueDays >= 7) {
            return ProjectRiskSeverity.HIGH;
        }
        return ProjectRiskSeverity.MEDIUM;
    }

    private static ProjectRiskSeverity bottleneckSeverity(int affectedTasks) {
        if (affectedTasks >= 8) {
            return ProjectRiskSeverity.CRITICAL;
        }
        if (affectedTasks >= 5) {
            return ProjectRiskSeverity.HIGH;
        }
        return ProjectRiskSeverity.MEDIUM;
    }

    private static List<UUID> downstreamOpenTasks(
            UUID startTaskId, Map<UUID, Set<UUID>> outgoing, Map<UUID, TaskSnapshot> taskById) {
        Set<UUID> traversed = new HashSet<>();
        Set<UUID> affected = new HashSet<>();
        ArrayDeque<UUID> queue = new ArrayDeque<>();
        queue.addAll(outgoing.getOrDefault(startTaskId, Set.of()));

        while (!queue.isEmpty()) {
            UUID next = queue.removeFirst();
            if (next.equals(startTaskId) || !traversed.add(next)) {
                continue;
            }
            TaskSnapshot task = taskById.get(next);
            if (task == null || !isOpen(task)) {
                continue;
            }
            affected.add(next);
            queue.addAll(outgoing.getOrDefault(next, Set.of()));
        }
        return sortedIds(affected);
    }

    private static List<UUID> sortedIds(Set<UUID> ids) {
        return ids.stream().sorted(Comparator.comparing(UUID::toString)).toList();
    }
}
