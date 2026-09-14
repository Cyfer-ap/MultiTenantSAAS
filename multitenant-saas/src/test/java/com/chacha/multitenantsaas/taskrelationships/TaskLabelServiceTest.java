package com.chacha.multitenantsaas.taskrelationships;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipChangeSink;
import com.chacha.multitenantsaas.taskrelationships.port.TaskRelationshipTaskGateway;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelAssignmentRepository;
import com.chacha.multitenantsaas.taskrelationships.repository.ProjectTaskLabelRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TaskLabelServiceTest {

    @Mock private TaskRelationshipTaskGateway taskGateway;
    @Mock private ProjectTaskLabelRepository labelRepository;
    @Mock private ProjectTaskLabelAssignmentRepository assignmentRepository;
    @Mock private TaskRelationshipChangeSink changeSink;
    @Mock private TaskRelationshipQueryService queryService;
    @Mock private Jwt jwt;

    private TaskLabelService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new TaskLabelService(
                        taskGateway,
                        labelRepository,
                        assignmentRepository,
                        changeSink,
                        queryService);
    }

    @Test
    void labelNamesAreNormalizedBeforeUniquenessCheck() {
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(activeProject());
        when(labelRepository.existsByTenant_IdAndProject_IdAndNormalizedName(
                        tenantId, projectId, "urgent review"))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.createLabel(
                                        tenantId,
                                        projectId,
                                        new ProjectTaskLabelRequest(
                                                "  Urgent   Review ", "#AA5500"),
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(labelRepository, never()).save(any());
    }

    @Test
    void rejectsMoreThanMaximumLabelsPerTask() {
        UUID taskId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        ProjectTask task = task(taskId);
        ProjectTaskLabel label = mock(ProjectTaskLabel.class);
        when(taskGateway.requireProject(tenantId, projectId)).thenReturn(activeProject());
        when(taskGateway.requireTask(tenantId, projectId, taskId)).thenReturn(task);
        when(labelRepository.findByTenant_IdAndProject_IdAndId(tenantId, projectId, labelId))
                .thenReturn(Optional.of(label));
        when(assignmentRepository.findByTenant_IdAndProject_IdAndTask_IdAndLabel_Id(
                        tenantId, projectId, taskId, labelId))
                .thenReturn(Optional.empty());
        when(assignmentRepository.countByTenant_IdAndProject_IdAndTask_Id(
                        tenantId, projectId, taskId))
                .thenReturn((long) TaskLabelService.MAX_LABELS_PER_TASK);

        assertThatThrownBy(() -> service.assignLabel(tenantId, projectId, taskId, labelId, jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");

        verify(assignmentRepository, never()).save(any());
    }

    private Project activeProject() {
        Project project = mock(Project.class);
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
