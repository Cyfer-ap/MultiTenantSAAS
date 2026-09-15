package com.chacha.multitenantsaas.projectsimulation;

import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.ConflictChangeType;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.DependencyChange;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.DependencyChangeType;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.DependencyConflict;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.Request;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.Response;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.Summary;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.TaskImpact;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.TaskOverride;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.WorkloadImpact;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationDependencySource;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationDependencySource.DependencySnapshot;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationTaskSource;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationTaskSource.AssigneeSnapshot;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationTaskSource.TaskSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProjectSimulationService {

    static final int MAX_TASKS = 500;
    static final int MAX_DEPENDENCIES = 1000;

    private final ProjectSimulationTaskSource taskSource;
    private final ProjectSimulationDependencySource dependencySource;

    public ProjectSimulationService(
            ProjectSimulationTaskSource taskSource,
            ProjectSimulationDependencySource dependencySource) {
        this.taskSource = taskSource;
        this.dependencySource = dependencySource;
    }

    public Response simulate(UUID tenantId, UUID projectId, Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Simulation request is required");
        }

        List<TaskSnapshot> taskSnapshots =
                taskSource.findProjectTasks(tenantId, projectId, MAX_TASKS + 1);
        if (taskSnapshots.size() > MAX_TASKS) {
            throw new IllegalArgumentException(
                    "Project simulation is limited to " + MAX_TASKS + " tasks");
        }

        List<DependencySnapshot> dependencySnapshots =
                dependencySource.findProjectDependencies(
                        tenantId, projectId, MAX_DEPENDENCIES + 1);
        if (dependencySnapshots.size() > MAX_DEPENDENCIES) {
            throw new IllegalArgumentException(
                    "Project simulation is limited to " + MAX_DEPENDENCIES + " dependencies");
        }

        Map<UUID, ScenarioTask> baselineTasks = new LinkedHashMap<>();
        for (TaskSnapshot snapshot : taskSnapshots) {
            if (baselineTasks.putIfAbsent(snapshot.taskId(), ScenarioTask.from(snapshot)) != null) {
                throw new IllegalStateException(
                        "Project simulation source returned duplicate task: " + snapshot.taskId());
            }
        }
        Map<UUID, ScenarioTask> simulatedTasks = new LinkedHashMap<>(baselineTasks);

        Set<Edge> baselineEdges = new LinkedHashSet<>();
        for (DependencySnapshot snapshot : dependencySnapshots) {
            requireKnownTask(baselineTasks, snapshot.blockingTaskId());
            requireKnownTask(baselineTasks, snapshot.dependentTaskId());
            baselineEdges.add(new Edge(snapshot.blockingTaskId(), snapshot.dependentTaskId()));
        }
        Set<Edge> simulatedEdges = new LinkedHashSet<>(baselineEdges);

        Set<UUID> directChangedTaskIds = new LinkedHashSet<>();
        Set<UUID> dueDateChangedTaskIds = new LinkedHashSet<>();
        Set<UUID> dependencyChangedTaskIds = new LinkedHashSet<>();
        Set<UUID> seenTaskOverrides = new HashSet<>();

        for (TaskOverride override : request.taskOverrides()) {
            if (!seenTaskOverrides.add(override.taskId())) {
                throw new IllegalArgumentException(
                        "A task may only be overridden once per simulation: " + override.taskId());
            }
            ScenarioTask current = requireKnownTask(simulatedTasks, override.taskId());
            validateOverrideShape(override);

            ScenarioTask updated = current;
            if (override.clearDueAt()) {
                updated = updated.withDueAt(null);
            } else if (override.dueAt() != null) {
                updated = updated.withDueAt(override.dueAt());
            }

            if (override.clearAssignee()) {
                updated = updated.withAssignee(null, null);
            } else if (override.assigneeUserId() != null) {
                AssigneeSnapshot assignee =
                        taskSource
                                .findAssignableAssignee(
                                        tenantId, projectId, override.assigneeUserId())
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Simulation assignee must be an active project member: "
                                                                + override.assigneeUserId()));
                updated = updated.withAssignee(assignee.userId(), assignee.displayName());
            }

            if (!Objects.equals(current.dueAt(), updated.dueAt())) {
                dueDateChangedTaskIds.add(current.taskId());
            }
            if (!current.sameScenarioValues(updated)) {
                directChangedTaskIds.add(current.taskId());
                simulatedTasks.put(current.taskId(), updated);
            }
        }

        Set<String> seenDependencyChanges = new HashSet<>();
        for (DependencyChange change : request.dependencyChanges()) {
            requireKnownTask(simulatedTasks, change.blockingTaskId());
            requireKnownTask(simulatedTasks, change.dependentTaskId());
            if (change.blockingTaskId().equals(change.dependentTaskId())) {
                throw new IllegalArgumentException("A task cannot depend on itself");
            }

            String changeKey =
                    change.type()
                            + ":"
                            + change.blockingTaskId()
                            + ":"
                            + change.dependentTaskId();
            if (!seenDependencyChanges.add(changeKey)) {
                throw new IllegalArgumentException("Duplicate dependency change: " + changeKey);
            }

            Edge edge = new Edge(change.blockingTaskId(), change.dependentTaskId());
            if (change.type() == DependencyChangeType.ADD) {
                if (!simulatedEdges.add(edge)) {
                    throw new IllegalArgumentException("Dependency already exists in the baseline");
                }
            } else if (!simulatedEdges.remove(edge)) {
                throw new IllegalArgumentException("Dependency does not exist in the baseline");
            }
            dependencyChangedTaskIds.add(change.dependentTaskId());
        }

        ensureAcyclic(simulatedTasks.keySet(), simulatedEdges);

        Set<UUID> downstreamSeeds = new LinkedHashSet<>(dueDateChangedTaskIds);
        downstreamSeeds.addAll(dependencyChangedTaskIds);
        Set<UUID> downstreamAffected = descendants(downstreamSeeds, simulatedEdges);
        downstreamAffected.removeAll(downstreamSeeds);

        Set<Edge> baselineConflicts = dependencyConflicts(baselineTasks, baselineEdges);
        Set<Edge> simulatedConflicts = dependencyConflicts(simulatedTasks, simulatedEdges);
        Set<Edge> newConflicts = new LinkedHashSet<>(simulatedConflicts);
        newConflicts.removeAll(baselineConflicts);
        Set<Edge> resolvedConflicts = new LinkedHashSet<>(baselineConflicts);
        resolvedConflicts.removeAll(simulatedConflicts);

        Set<UUID> impactedTaskIds = new LinkedHashSet<>(directChangedTaskIds);
        impactedTaskIds.addAll(dependencyChangedTaskIds);
        impactedTaskIds.addAll(downstreamAffected);

        List<TaskImpact> taskImpacts =
                impactedTaskIds.stream()
                        .map(
                                taskId ->
                                        taskImpact(
                                                baselineTasks.get(taskId),
                                                simulatedTasks.get(taskId),
                                                directChangedTaskIds.contains(taskId),
                                                dependencyChangedTaskIds.contains(taskId),
                                                downstreamAffected.contains(taskId),
                                                simulatedConflicts))
                        .sorted(
                                Comparator.comparing(TaskImpact::title, String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(TaskImpact::taskId))
                        .toList();

        List<DependencyConflict> conflicts =
                buildConflictComparison(
                        baselineTasks,
                        simulatedTasks,
                        baselineConflicts,
                        simulatedConflicts);
        List<WorkloadImpact> workloadImpacts = workloadImpacts(baselineTasks, simulatedTasks);

        int reassignedTasks =
                (int)
                        baselineTasks.values().stream()
                                .filter(
                                        task ->
                                                !Objects.equals(
                                                        task.assigneeUserId(),
                                                        simulatedTasks
                                                                .get(task.taskId())
                                                                .assigneeUserId()))
                                .count();

        Summary summary =
                new Summary(
                        baselineTasks.size(),
                        directChangedTaskIds.size(),
                        request.dependencyChanges().size(),
                        downstreamAffected.size(),
                        baselineConflicts.size(),
                        simulatedConflicts.size(),
                        newConflicts.size(),
                        resolvedConflicts.size(),
                        reassignedTasks);

        List<String> notes = new ArrayList<>();
        notes.add("Simulation is advisory only; no project data was mutated.");
        notes.add(
                "Schedule impact is based on due-date consistency across dependency edges; task duration is not inferred.");
        if (!newConflicts.isEmpty()) {
            notes.add(
                    newConflicts.size()
                            + " new dependency deadline conflict(s) would be introduced.");
        }
        if (!resolvedConflicts.isEmpty()) {
            notes.add(
                    resolvedConflicts.size()
                            + " existing dependency deadline conflict(s) would be resolved.");
        }

        return new Response(
                projectId,
                Instant.now(),
                summary,
                taskImpacts,
                conflicts,
                workloadImpacts,
                List.copyOf(notes));
    }

    private void validateOverrideShape(TaskOverride override) {
        if (override.clearDueAt() && override.dueAt() != null) {
            throw new IllegalArgumentException(
                    "A due date override cannot set and clear the due date at the same time");
        }
        if (override.clearAssignee() && override.assigneeUserId() != null) {
            throw new IllegalArgumentException(
                    "An assignee override cannot set and clear the assignee at the same time");
        }
    }

    private ScenarioTask requireKnownTask(Map<UUID, ScenarioTask> tasks, UUID taskId) {
        ScenarioTask task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task is not part of this project: " + taskId);
        }
        return task;
    }

    private void ensureAcyclic(Set<UUID> taskIds, Set<Edge> edges) {
        Map<UUID, Integer> incoming = new HashMap<>();
        Map<UUID, List<UUID>> outgoing = new HashMap<>();
        for (UUID taskId : taskIds) {
            incoming.put(taskId, 0);
        }
        for (Edge edge : edges) {
            incoming.merge(edge.dependentTaskId(), 1, Integer::sum);
            outgoing.computeIfAbsent(edge.blockingTaskId(), ignored -> new ArrayList<>())
                    .add(edge.dependentTaskId());
        }

        ArrayDeque<UUID> queue = new ArrayDeque<>();
        incoming.forEach(
                (taskId, count) -> {
                    if (count == 0) {
                        queue.addLast(taskId);
                    }
                });

        int visited = 0;
        while (!queue.isEmpty()) {
            UUID current = queue.removeFirst();
            visited++;
            for (UUID dependent : outgoing.getOrDefault(current, List.of())) {
                int remaining = incoming.merge(dependent, -1, Integer::sum);
                if (remaining == 0) {
                    queue.addLast(dependent);
                }
            }
        }
        if (visited != taskIds.size()) {
            throw new IllegalArgumentException(
                    "The simulated dependency graph must remain acyclic");
        }
    }

    private Set<UUID> descendants(Set<UUID> seeds, Set<Edge> edges) {
        if (seeds.isEmpty()) {
            return Set.of();
        }
        Map<UUID, List<UUID>> outgoing = new HashMap<>();
        for (Edge edge : edges) {
            outgoing.computeIfAbsent(edge.blockingTaskId(), ignored -> new ArrayList<>())
                    .add(edge.dependentTaskId());
        }
        Set<UUID> affected = new LinkedHashSet<>();
        ArrayDeque<UUID> queue = new ArrayDeque<>(seeds);
        while (!queue.isEmpty()) {
            UUID current = queue.removeFirst();
            for (UUID dependent : outgoing.getOrDefault(current, List.of())) {
                if (affected.add(dependent)) {
                    queue.addLast(dependent);
                }
            }
        }
        return affected;
    }

    private Set<Edge> dependencyConflicts(Map<UUID, ScenarioTask> tasks, Set<Edge> edges) {
        Set<Edge> conflicts = new LinkedHashSet<>();
        for (Edge edge : edges) {
            ScenarioTask blocker = tasks.get(edge.blockingTaskId());
            ScenarioTask dependent = tasks.get(edge.dependentTaskId());
            if (blocker == null
                    || dependent == null
                    || !blocker.isOpen()
                    || !dependent.isOpen()
                    || blocker.dueAt() == null
                    || dependent.dueAt() == null) {
                continue;
            }
            if (blocker.dueAt().isAfter(dependent.dueAt())) {
                conflicts.add(edge);
            }
        }
        return conflicts;
    }

    private TaskImpact taskImpact(
            ScenarioTask baseline,
            ScenarioTask simulated,
            boolean directChange,
            boolean dependencyChanged,
            boolean downstreamAffected,
            Set<Edge> simulatedConflicts) {
        Long dueDateShiftHours = null;
        if (baseline.dueAt() != null && simulated.dueAt() != null) {
            dueDateShiftHours = Duration.between(baseline.dueAt(), simulated.dueAt()).toHours();
        }
        boolean conflict =
                simulatedConflicts.stream()
                        .anyMatch(
                                edge ->
                                        edge.blockingTaskId().equals(baseline.taskId())
                                                || edge.dependentTaskId()
                                                        .equals(baseline.taskId()));
        return new TaskImpact(
                baseline.taskId(),
                baseline.title(),
                directChange,
                dependencyChanged,
                downstreamAffected,
                baseline.dueAt(),
                simulated.dueAt(),
                dueDateShiftHours,
                baseline.assigneeUserId(),
                baseline.assigneeName(),
                simulated.assigneeUserId(),
                simulated.assigneeName(),
                conflict);
    }

    private List<DependencyConflict> buildConflictComparison(
            Map<UUID, ScenarioTask> baselineTasks,
            Map<UUID, ScenarioTask> simulatedTasks,
            Set<Edge> baselineConflicts,
            Set<Edge> simulatedConflicts) {
        Set<Edge> all = new LinkedHashSet<>(baselineConflicts);
        all.addAll(simulatedConflicts);
        return all.stream()
                .map(
                        edge -> {
                            ScenarioTask baselineBlocker = baselineTasks.get(edge.blockingTaskId());
                            ScenarioTask baselineDependent = baselineTasks.get(edge.dependentTaskId());
                            ScenarioTask simulatedBlocker = simulatedTasks.get(edge.blockingTaskId());
                            ScenarioTask simulatedDependent = simulatedTasks.get(edge.dependentTaskId());
                            ConflictChangeType changeType =
                                    !baselineConflicts.contains(edge)
                                            ? ConflictChangeType.NEW
                                            : !simulatedConflicts.contains(edge)
                                                    ? ConflictChangeType.RESOLVED
                                                    : ConflictChangeType.UNCHANGED;
                            return new DependencyConflict(
                                    edge.blockingTaskId(),
                                    baselineBlocker.title(),
                                    edge.dependentTaskId(),
                                    baselineDependent.title(),
                                    changeType,
                                    baselineBlocker.dueAt(),
                                    baselineDependent.dueAt(),
                                    simulatedBlocker.dueAt(),
                                    simulatedDependent.dueAt());
                        })
                .sorted(
                        Comparator.comparing(
                                        DependencyConflict::blockingTaskTitle,
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        DependencyConflict::dependentTaskTitle,
                                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<WorkloadImpact> workloadImpacts(
            Map<UUID, ScenarioTask> baselineTasks, Map<UUID, ScenarioTask> simulatedTasks) {
        Map<UUID, Integer> baselineCounts = openTaskCounts(baselineTasks);
        Map<UUID, Integer> simulatedCounts = openTaskCounts(simulatedTasks);
        Map<UUID, String> names = new HashMap<>();
        baselineTasks.values().forEach(task -> names.put(task.assigneeUserId(), task.assigneeName()));
        simulatedTasks.values().forEach(task -> names.put(task.assigneeUserId(), task.assigneeName()));

        Set<UUID> users = new LinkedHashSet<>(baselineCounts.keySet());
        users.addAll(simulatedCounts.keySet());
        return users.stream()
                .map(
                        userId -> {
                            int baseline = baselineCounts.getOrDefault(userId, 0);
                            int simulated = simulatedCounts.getOrDefault(userId, 0);
                            String displayName = userId == null ? "Unassigned" : names.get(userId);
                            return new WorkloadImpact(
                                    userId,
                                    displayName == null ? userId.toString() : displayName,
                                    baseline,
                                    simulated,
                                    simulated - baseline);
                        })
                .filter(impact -> impact.delta() != 0)
                .sorted(
                        Comparator.comparing(
                                WorkloadImpact::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<UUID, Integer> openTaskCounts(Map<UUID, ScenarioTask> tasks) {
        Map<UUID, Integer> counts = new HashMap<>();
        for (ScenarioTask task : tasks.values()) {
            if (task.isOpen()) {
                counts.merge(task.assigneeUserId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private record Edge(UUID blockingTaskId, UUID dependentTaskId) {}

    private record ScenarioTask(
            UUID taskId,
            String title,
            String status,
            UUID assigneeUserId,
            String assigneeName,
            Instant dueAt) {

        private static ScenarioTask from(TaskSnapshot snapshot) {
            return new ScenarioTask(
                    snapshot.taskId(),
                    snapshot.title(),
                    snapshot.status(),
                    snapshot.assigneeUserId(),
                    snapshot.assigneeName(),
                    snapshot.dueAt());
        }

        private ScenarioTask withDueAt(Instant nextDueAt) {
            return new ScenarioTask(
                    taskId, title, status, assigneeUserId, assigneeName, nextDueAt);
        }

        private ScenarioTask withAssignee(UUID nextAssigneeUserId, String nextAssigneeName) {
            return new ScenarioTask(
                    taskId, title, status, nextAssigneeUserId, nextAssigneeName, dueAt);
        }

        private boolean sameScenarioValues(ScenarioTask other) {
            return Objects.equals(dueAt, other.dueAt)
                    && Objects.equals(assigneeUserId, other.assigneeUserId);
        }

        private boolean isOpen() {
            return !"COMPLETED".equals(status) && !"CANCELLED".equals(status);
        }
    }
}
