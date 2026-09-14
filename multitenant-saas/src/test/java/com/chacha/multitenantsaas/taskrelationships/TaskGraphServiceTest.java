package com.chacha.multitenantsaas.taskrelationships;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.taskrelationships.dto.TaskRelationshipsResponse;
import com.chacha.multitenantsaas.taskrelationships.entity.TaskDependency;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipChangeSink;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipTaskGateway;
import com.chacha.multitenantsaas.taskrelationships.repository.TaskDependencyRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TaskGraphServiceTest {

    @Mock private TaskRelationshipTaskGateway taskGateway;
    @Mock private TaskDependencyRepository dependencyRepository;
    @Mock private TaskRelationshipChangeSink changeSink;
    @Mock private TaskRelationshipQueryService queryService;
    @Mock private Jwt jwt;

    private TaskGraphService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new TaskGraphService(
                        taskGateway, dependencyRepository, changeSink, queryService);
    }

    @Test
    void rejectsSelfParent() {
        UUID taskId = UUID.randomUUID();
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(activeProject());
        when(taskGateway.requireTask(tenantId, projectId, taskId)).thenReturn(task(taskId));

        assertThatThrownBy(
                        () -> service.updateParent(tenantId, projectId, taskId, taskId, jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own parent");

        verify(taskGateway, never()).save(any());
    }

    @Test
    void rejectsParentThatCreatesAncestryCycle() {
        UUID childId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        ProjectTask child = task(childId);
        ProjectTask parent = task(parentId);
        when(parent.getParentTask()).thenReturn(child);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(activeProject());
        when(taskGateway.requireTask(tenantId, projectId, childId)).thenReturn(child);
        when(taskGateway.requireTask(tenantId, projectId, parentId)).thenReturn(parent);

        assertThatThrownBy(
                        () -> service.updateParent(tenantId, projectId, childId, parentId, jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hierarchy cycle");

        verify(taskGateway, never()).save(any());
    }

    @Test
    void rejectsDependencyThatCreatesDirectedCycle() {
        UUID dependentId = UUID.randomUUID();
        UUID blockingId = UUID.randomUUID();
        ProjectTask dependent = task(dependentId);
        ProjectTask blocking = task(blockingId);
        TaskDependency reversePath = mock(TaskDependency.class);
        when(reversePath.getBlockingTask()).thenReturn(dependent);
        when(reversePath.getDependentTask()).thenReturn(blocking);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(activeProject());
        when(taskGateway.requireTask(tenantId, projectId, dependentId)).thenReturn(dependent);
        when(taskGateway.requireTask(tenantId, projectId, blockingId)).thenReturn(blocking);
        when(dependencyRepository
                        .existsByTenant_IdAndProject_IdAndBlockingTask_IdAndDependentTask_Id(
                                tenantId, projectId, blockingId, dependentId))
                .thenReturn(false);
        when(dependencyRepository.findByTenant_IdAndProject_Id(
                        eq(tenantId), eq(projectId), any(Pageable.class)))
                .thenReturn(List.of(reversePath));

        assertThatThrownBy(
                        () ->
                                service.addDependency(
                                        tenantId,
                                        projectId,
                                        dependentId,
                                        blockingId,
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directed cycle");

        verify(dependencyRepository, never()).save(any());
    }

    @Test
    void duplicateDependencyIsIdempotent() {
        UUID dependentId = UUID.randomUUID();
        UUID blockingId = UUID.randomUUID();
        ProjectTask dependent = task(dependentId);
        ProjectTask blocking = task(blockingId);
        TaskRelationshipsResponse expected = mock(TaskRelationshipsResponse.class);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(activeProject());
        when(taskGateway.requireTask(tenantId, projectId, dependentId)).thenReturn(dependent);
        when(taskGateway.requireTask(tenantId, projectId, blockingId)).thenReturn(blocking);
        when(dependencyRepository
                        .existsByTenant_IdAndProject_IdAndBlockingTask_IdAndDependentTask_Id(
                                tenantId, projectId, blockingId, dependentId))
                .thenReturn(true);
        when(queryService.getRelationships(tenantId, projectId, dependentId)).thenReturn(expected);

        var result = service.addDependency(tenantId, projectId, dependentId, blockingId, jwt);

        assertThat(result).isSameAs(expected);
        verify(dependencyRepository, never()).save(any());
        verify(changeSink, never())
                .record(any(), any(), any(), any(), any(), any(), any());
    }

    private Project activeProject() {
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(projectId);
        when(project.getStatus()).thenReturn(ProjectStatus.ACTIVE);
        return project;
    }

    private ProjectTask task(UUID id) {
        ProjectTask task = mock(ProjectTask.class);
        when(task.getId()).thenReturn(id);
        when(task.getStatus()).thenReturn(ProjectTaskStatus.TODO);
        return task;
    }
}
