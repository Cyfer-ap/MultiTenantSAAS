package com.chacha.multitenantsaas.projectsimulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.DependencyChange;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.DependencyChangeType;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.Request;
import com.chacha.multitenantsaas.projectsimulation.dto.ProjectSimulationDtos.TaskOverride;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationDependencySource;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationDependencySource.DependencySnapshot;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationTaskSource;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationTaskSource.AssigneeSnapshot;
import com.chacha.multitenantsaas.projectsimulation.spi.ProjectSimulationTaskSource.TaskSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectSimulationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private ProjectSimulationTaskSource taskSource;
    private ProjectSimulationDependencySource dependencySource;
    private ProjectSimulationService service;

    @BeforeEach
    void setUp() {
        taskSource = mock(ProjectSimulationTaskSource.class);
        dependencySource = mock(ProjectSimulationDependencySource.class);
        service = new ProjectSimulationService(taskSource, dependencySource);
    }

    @Test
    void reportsDownstreamExposureAndNewDependencyDeadlineConflict() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID owner = UUID.randomUUID();

        when(taskSource.findProjectTasks(
                        tenantId, projectId, ProjectSimulationService.MAX_TASKS + 1))
                .thenReturn(
                        List.of(
                                task(first, "Design", owner, "Owner", "2026-09-20T10:00:00Z"),
                                task(second, "Build", owner, "Owner", "2026-09-22T10:00:00Z"),
                                task(third, "Launch", owner, "Owner", "2026-09-25T10:00:00Z")));
        when(dependencySource.findProjectDependencies(
                        tenantId, projectId, ProjectSimulationService.MAX_DEPENDENCIES + 1))
                .thenReturn(
                        List.of(
                                new DependencySnapshot(first, second),
                                new DependencySnapshot(second, third)));

        var response =
                service.simulate(
                        tenantId,
                        projectId,
                        new Request(
                                List.of(
                                        new TaskOverride(
                                                first,
                                                Instant.parse("2026-09-24T10:00:00Z"),
                                                false,
                                                null,
                                                false)),
                                List.of()));

        assertThat(response.summary().directTaskChanges()).isEqualTo(1);
        assertThat(response.summary().downstreamAffectedTasks()).isEqualTo(2);
        assertThat(response.summary().newDependencyConflicts()).isEqualTo(1);
        assertThat(response.dependencyConflicts())
                .singleElement()
                .satisfies(
                        conflict -> {
                            assertThat(conflict.blockingTaskId()).isEqualTo(first);
                            assertThat(conflict.dependentTaskId()).isEqualTo(second);
                            assertThat(conflict.changeType().name()).isEqualTo("NEW");
                        });
        assertThat(response.taskImpacts())
                .filteredOn(impact -> impact.taskId().equals(first))
                .singleElement()
                .satisfies(
                        impact -> {
                            assertThat(impact.directChange()).isTrue();
                            assertThat(impact.dueDateShiftHours()).isEqualTo(96L);
                        });
        assertThat(response.taskImpacts())
                .filteredOn(impact -> impact.taskId().equals(third))
                .singleElement()
                .satisfies(impact -> assertThat(impact.downstreamAffected()).isTrue());
    }

    @Test
    void reportsAssigneeWorkloadDeltaWithoutTreatingItAsSchedulePropagation() {
        UUID taskId = UUID.randomUUID();
        UUID currentOwner = UUID.randomUUID();
        UUID nextOwner = UUID.randomUUID();

        when(taskSource.findProjectTasks(
                        tenantId, projectId, ProjectSimulationService.MAX_TASKS + 1))
                .thenReturn(
                        List.of(
                                task(
                                        taskId,
                                        "Review",
                                        currentOwner,
                                        "Current owner",
                                        "2026-09-20T10:00:00Z")));
        when(dependencySource.findProjectDependencies(
                        tenantId, projectId, ProjectSimulationService.MAX_DEPENDENCIES + 1))
                .thenReturn(List.of());
        when(taskSource.findAssignableAssignee(tenantId, projectId, nextOwner))
                .thenReturn(Optional.of(new AssigneeSnapshot(nextOwner, "Next owner")));

        var response =
                service.simulate(
                        tenantId,
                        projectId,
                        new Request(
                                List.of(
                                        new TaskOverride(
                                                taskId, null, false, nextOwner, false)),
                                List.of()));

        assertThat(response.summary().reassignedTasks()).isEqualTo(1);
        assertThat(response.summary().downstreamAffectedTasks()).isZero();
        assertThat(response.workloadImpacts()).hasSize(2);
        assertThat(response.workloadImpacts())
                .filteredOn(impact -> nextOwner.equals(impact.userId()))
                .singleElement()
                .satisfies(
                        impact -> {
                            assertThat(impact.baselineOpenTasks()).isZero();
                            assertThat(impact.simulatedOpenTasks()).isEqualTo(1);
                            assertThat(impact.delta()).isEqualTo(1);
                        });
    }

    @Test
    void rejectsDependencyChangesThatCreateACycle() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID owner = UUID.randomUUID();

        when(taskSource.findProjectTasks(
                        tenantId, projectId, ProjectSimulationService.MAX_TASKS + 1))
                .thenReturn(
                        List.of(
                                task(first, "One", owner, "Owner", "2026-09-20T10:00:00Z"),
                                task(second, "Two", owner, "Owner", "2026-09-21T10:00:00Z"),
                                task(third, "Three", owner, "Owner", "2026-09-22T10:00:00Z")));
        when(dependencySource.findProjectDependencies(
                        tenantId, projectId, ProjectSimulationService.MAX_DEPENDENCIES + 1))
                .thenReturn(
                        List.of(
                                new DependencySnapshot(first, second),
                                new DependencySnapshot(second, third)));

        var request =
                new Request(
                        List.of(),
                        List.of(
                                new DependencyChange(
                                        DependencyChangeType.ADD, third, first)));

        assertThatThrownBy(() -> service.simulate(tenantId, projectId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acyclic");
    }

    private TaskSnapshot task(
            UUID taskId, String title, UUID assigneeId, String assigneeName, String dueAt) {
        return new TaskSnapshot(
                taskId,
                title,
                "TODO",
                assigneeId,
                assigneeName,
                Instant.parse(dueAt));
    }
}
