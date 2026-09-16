package com.chacha.multitenantsaas.projectrisk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskDependencySource;
import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskDependencySource.DependencySnapshot;
import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskTaskSource;
import com.chacha.multitenantsaas.projectrisk.spi.ProjectRiskTaskSource.TaskSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectRiskServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private ProjectRiskTaskSource taskSource;
    private ProjectRiskDependencySource dependencySource;
    private ProjectRiskService service;

    @BeforeEach
    void setUp() {
        taskSource = mock(ProjectRiskTaskSource.class);
        dependencySource = mock(ProjectRiskDependencySource.class);
        service =
                new ProjectRiskService(
                        taskSource, dependencySource, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void reportsExplainableProjectRiskSignalsWithoutOpaqueWorkloadScoring() {
        UUID owner = UUID.randomUUID();
        UUID blocker = UUID.randomUUID();
        UUID dependent = UUID.randomUUID();
        UUID downstream = UUID.randomUUID();
        UUID staleUnassigned = UUID.randomUUID();
        UUID explicitlyBlocked = UUID.randomUUID();

        when(taskSource.findProjectTasks(tenantId, projectId, ProjectRiskService.MAX_TASKS + 1))
                .thenReturn(
                        List.of(
                                task(
                                        blocker,
                                        "Release API",
                                        "IN_PROGRESS",
                                        "URGENT",
                                        owner,
                                        "2026-09-01T12:00:00Z",
                                        "2026-09-15T12:00:00Z"),
                                task(
                                        dependent,
                                        "Mobile integration",
                                        "TODO",
                                        "MEDIUM",
                                        owner,
                                        "2026-09-20T12:00:00Z",
                                        "2026-09-15T12:00:00Z"),
                                task(
                                        downstream,
                                        "Launch review",
                                        "TODO",
                                        "MEDIUM",
                                        owner,
                                        "2026-09-25T12:00:00Z",
                                        "2026-09-15T12:00:00Z"),
                                task(
                                        staleUnassigned,
                                        "Security sign-off",
                                        "TODO",
                                        "HIGH",
                                        null,
                                        "2026-09-30T12:00:00Z",
                                        "2026-08-20T12:00:00Z"),
                                task(
                                        explicitlyBlocked,
                                        "Partner approval",
                                        "BLOCKED",
                                        "MEDIUM",
                                        owner,
                                        "2026-09-30T12:00:00Z",
                                        "2026-09-15T12:00:00Z")));
        when(dependencySource.findProjectDependencies(
                        tenantId, projectId, ProjectRiskService.MAX_DEPENDENCIES + 1))
                .thenReturn(
                        List.of(
                                new DependencySnapshot(blocker, dependent),
                                new DependencySnapshot(dependent, downstream),
                                new DependencySnapshot(blocker, staleUnassigned)));

        var response = service.analyze(tenantId, projectId);

        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.generatedAt()).isEqualTo(NOW);
        assertThat(response.riskLevel()).isEqualTo(ProjectRiskSeverity.CRITICAL);
        assertThat(response.staleAfterDays()).isEqualTo(14);
        assertThat(response.summary().totalTasks()).isEqualTo(5);
        assertThat(response.summary().openTasks()).isEqualTo(5);
        assertThat(response.summary().overdueTasks()).isEqualTo(1);
        assertThat(response.summary().blockedTasks()).isEqualTo(4);
        assertThat(response.summary().staleTasks()).isEqualTo(1);
        assertThat(response.summary().unassignedCriticalTasks()).isEqualTo(1);
        assertThat(response.summary().dependencyBottlenecks()).isEqualTo(1);
        assertThat(response.signals())
                .extracting(signal -> signal.type())
                .contains(
                        ProjectRiskSignalType.OVERDUE_TASK,
                        ProjectRiskSignalType.BLOCKED_TASK,
                        ProjectRiskSignalType.STALE_TASK,
                        ProjectRiskSignalType.UNASSIGNED_CRITICAL_TASK,
                        ProjectRiskSignalType.DEPENDENCY_BOTTLENECK);
        assertThat(response.signals())
                .filteredOn(signal -> signal.type() == ProjectRiskSignalType.DEPENDENCY_BOTTLENECK)
                .singleElement()
                .satisfies(
                        signal -> {
                            assertThat(signal.taskId()).isEqualTo(blocker);
                            assertThat(signal.affectedTaskCount()).isEqualTo(3);
                            assertThat(signal.relatedTaskIds())
                                    .containsExactlyInAnyOrder(
                                            dependent, downstream, staleUnassigned);
                        });
        assertThat(response.limitations())
                .anyMatch(message -> message.contains("no explicit capacity or availability"));
    }

    @Test
    void completedDependencyBlockersDoNotCreateBlockedOrBottleneckRisk() {
        UUID owner = UUID.randomUUID();
        UUID completed = UUID.randomUUID();
        UUID dependent = UUID.randomUUID();

        when(taskSource.findProjectTasks(tenantId, projectId, ProjectRiskService.MAX_TASKS + 1))
                .thenReturn(
                        List.of(
                                task(
                                        completed,
                                        "Completed prerequisite",
                                        "COMPLETED",
                                        "HIGH",
                                        owner,
                                        "2026-09-01T12:00:00Z",
                                        "2026-09-15T12:00:00Z"),
                                task(
                                        dependent,
                                        "Ready work",
                                        "TODO",
                                        "MEDIUM",
                                        owner,
                                        "2026-09-30T12:00:00Z",
                                        "2026-09-15T12:00:00Z")));
        when(dependencySource.findProjectDependencies(
                        tenantId, projectId, ProjectRiskService.MAX_DEPENDENCIES + 1))
                .thenReturn(List.of(new DependencySnapshot(completed, dependent)));

        var response = service.analyze(tenantId, projectId);

        assertThat(response.riskLevel()).isEqualTo(ProjectRiskSeverity.NONE);
        assertThat(response.summary().blockedTasks()).isZero();
        assertThat(response.summary().dependencyBottlenecks()).isZero();
        assertThat(response.signals()).isEmpty();
    }

    private TaskSnapshot task(
            UUID taskId,
            String title,
            String status,
            String priority,
            UUID assignee,
            String dueAt,
            String updatedAt) {
        return new TaskSnapshot(
                taskId,
                title,
                status,
                priority,
                assignee,
                dueAt == null ? null : Instant.parse(dueAt),
                Instant.parse(updatedAt));
    }
}
