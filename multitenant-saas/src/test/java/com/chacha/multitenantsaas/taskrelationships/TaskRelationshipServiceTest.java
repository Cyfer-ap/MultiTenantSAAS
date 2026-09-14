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
import com.chacha.multitenantsaas.taskrelationships.dto.ProjectTaskLabelRequest;
import com.chacha.multitenantsaas.taskrelationships.entity.ProjectTaskLabel;
import com.chacha.multitenantsaas.taskrelationships.entity.TaskDependency;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipChangeSink;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipTaskGateway;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelAssignmentRepository;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelRepository;
import com.chacha.multitenantsaas.taskrelationships.repository.TaskDependencyRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TaskRelationshipServiceTest {

    @Mock private TaskRelationshipTaskGateway taskGateway;
    @Mock private TaskDependencyRepository dependencyRepository;
    @Mock private ProjectTaskLabelRepository labelRepository;
    @Mock private ProjectTaskLabelAssignmentRepository assignmentRepository;
    @Mock private TaskRelationshipChangeSink changeSink;
    @Mock private Jwt jwt;

    private TaskRelationshipService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new TaskRelationshipService(
                        taskGateway,
                        dependencyRepository,
                        labelRepository,
                        assignmentRepository,
                        changeSink);
    }

    @Test
    void rejectsSelfParent() {
        UUID taskId = UUID.randomUUID();
        Project project = activeProject();
        ProjectTask task = task(taskId);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(project);
        when(taskGateway.requireTask(tenantId, projectId, taskId)).thenReturn(task);

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
        Project project = activeProject();
        ProjectTask child = task(childId);
        ProjectTask parent = task(parentId);
        when(parent.getParentTask()).thenReturn(child);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(project);
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
        Project project = activeProject();
        ProjectTask dependent = task(dependentId);
        ProjectTask blocking = task(blockingId);
        TaskDependency existingReversePath = mock(TaskDependency.class);
        when(existingReversePath.getBlockingTask()).thenReturn(dependent);
        when(existingReversePath.getDependentTask()).thenReturn(blocking);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(project);
        when(taskGateway.requireTask(tenantId, projectId, dependentId)).thenReturn(dependent);
        when(taskGateway.requireTask(tenantId, projectId, blockingId)).thenReturn(blocking);
        when(dependencyRepository
                        .existsByTenant_IdAndProject_IdAndBlockingTask_IdAndDependentTask_Id(
                                tenantId, projectId, blockingId, dependentId))
                .thenReturn(false);
        when(dependencyRepository.findByTenant_IdAndProject_Id(
                        eq(tenantId), eq(projectId), any(Pageable.class)))
                .thenReturn(List.of(existingReversePath));

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
        Project project = activeProject();
        ProjectTask dependent = task(dependentId);
        ProjectTask blocking = task(blockingId);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(project);
        when(taskGateway.requireTask(tenantId, projectId, dependentId)).thenReturn(dependent);
        when(taskGateway.requireTask(tenantId, projectId, blockingId)).thenReturn(blocking);
        when(dependencyRepository
                        .existsByTenant_IdAndProject_IdAndBlockingTask_IdAndDependentTask_Id(
                                tenantId, projectId, blockingId, dependentId))
                .thenReturn(true);
        stubEmptyRelationshipRead(dependentId);

        var response =
                service.addDependency(tenantId, projectId, dependentId, blockingId, jwt);

        assertThat(response.blockers()).isEmpty();
        verify(dependencyRepository, never()).save(any());
        verify(changeSink, never())
                .record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void labelNamesAreNormalizedBeforeUniquenessCheck() {
        Project project = activeProject();
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(project);
        when(labelRepository.existsByTenant_IdAndProject_IdAndNormalizedName(
                        tenantId, projectId, "urgent review"))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.createLabel(
                                        tenantId,
                                        projectId,
                                        new ProjectTaskLabelRequest("  Urgent   Review ", "#AA5500"),
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(labelRepository, never()).save(any());
    }

    @Test
    void rejectsMoreThanMaximumLabelsPerTask() {
        UUID taskId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        Project project = activeProject();
        ProjectTask task = task(taskId);
        ProjectTaskLabel label = mock(ProjectTaskLabel.class);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(project);
        when(taskGateway.requireTask(tenantId, projectId, taskId)).thenReturn(task);
        when(labelRepository.findByTenant_IdAndProject_IdAndId(tenantId, projectId, labelId))
                .thenReturn(Optional.of(label));
        when(assignmentRepository.findByTenant_IdAndProject_IdAndTask_IdAndLabel_Id(
                        tenantId, projectId, taskId, labelId))
                .thenReturn(Optional.empty());
        when(assignmentRepository.countByTenant_IdAndProject_IdAndTask_Id(
                        tenantId, projectId, taskId))
                .thenReturn((long) TaskRelationshipService.MAX_LABELS_PER_TASK);

        assertThatThrownBy(
                        () -> service.assignLabel(tenantId, projectId, taskId, labelId, jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");

        verify(assignmentRepository, never()).save(any());
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

    private void stubEmptyRelationshipRead(UUID taskId) {
        when(taskGateway.findChildren(
                        tenantId,
                        projectId,
                        taskId,
                        TaskRelationshipService.MAX_RELATED_TASKS + 1))
                .thenReturn(List.of());
        when(dependencyRepository
                        .findByTenant_IdAndProject_IdAndDependentTask_IdOrderByCreatedAtAsc(
                                eq(tenantId), eq(projectId), eq(taskId), any(Pageable.class)))
                .thenReturn(List.of());
        when(dependencyRepository
                        .findByTenant_IdAndProject_IdAndBlockingTask_IdOrderByCreatedAtAsc(
                                eq(tenantId), eq(projectId), eq(taskId), any(Pageable.class)))
                .thenReturn(List.of());
        when(assignmentRepository.findByTenant_IdAndProject_IdAndTask_IdOrderByAssignedAtAsc(
                        tenantId, projectId, taskId))
                .thenReturn(List.of());
    }
}
