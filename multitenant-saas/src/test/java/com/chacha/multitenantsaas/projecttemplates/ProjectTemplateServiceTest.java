package com.chacha.multitenantsaas.projecttemplates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationCommand;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationPort;
import com.chacha.multitenantsaas.projects.creation.ProjectCreationResult;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationCommand;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ProjectTemplateServiceTest {

    @Mock private ProjectTemplateRepository templateRepository;
    @Mock private ProjectTemplateTaskRepository taskRepository;
    @Mock private CurrentActorService currentActorService;
    @Mock private ProjectCreationPort projectCreationPort;
    @Mock private TaskCreationPort taskCreationPort;
    @Mock private Jwt jwt;

    @Test
    void instantiateCreatesProjectThenDeterministicTaskSnapshots() {
        UUID tenantId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-15T00:00:00Z");
        ProjectTemplate template = org.mockito.Mockito.mock(ProjectTemplate.class);
        ProjectTemplateTask first = org.mockito.Mockito.mock(ProjectTemplateTask.class);
        ProjectTemplateTask second = org.mockito.Mockito.mock(ProjectTemplateTask.class);
        AppUser actor = org.mockito.Mockito.mock(AppUser.class);

        when(templateRepository.findByTenantIdAndId(tenantId, templateId))
                .thenReturn(Optional.of(template));
        when(template.getName()).thenReturn("Launch kit");
        when(template.getProjectNameSeed()).thenReturn("Launch workspace");
        when(template.getProjectDescription()).thenReturn("Prepared from template");
        when(template.getInitialStatus()).thenReturn(ProjectStatus.ACTIVE);
        when(taskRepository.findByTenantIdAndTemplateIdOrderByPositionIndexAsc(
                        tenantId, templateId))
                .thenReturn(List.of(first, second));
        stubTask(first, "Plan", ProjectTaskPriority.HIGH, 60L);
        stubTask(second, "Ship", ProjectTaskPriority.MEDIUM, null);
        when(currentActorService.getRequiredActiveActor(tenantId, jwt)).thenReturn(actor);
        when(actor.getId()).thenReturn(actorId);
        when(projectCreationPort.createProject(any(ProjectCreationCommand.class)))
                .thenReturn(
                        new ProjectCreationResult(
                                projectId,
                                tenantId,
                                "Launch workspace",
                                "Prepared from template",
                                ProjectStatus.ACTIVE,
                                actorId,
                                "Owner",
                                "owner@example.test",
                                createdAt,
                                createdAt));
        when(taskCreationPort.createTask(any(TaskCreationCommand.class)))
                .thenReturn(new TaskCreationResult(UUID.randomUUID(), createdAt));

        ProjectTemplateDtos.InstantiateResponse response =
                service().instantiate(tenantId, templateId, null, jwt);

        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.tasksCreated()).isEqualTo(2);

        ArgumentCaptor<ProjectCreationCommand> projectCommand =
                ArgumentCaptor.forClass(ProjectCreationCommand.class);
        verify(projectCreationPort).createProject(projectCommand.capture());
        assertThat(projectCommand.getValue().actorUserId()).isEqualTo(actorId);
        assertThat(projectCommand.getValue().name()).isEqualTo("Launch workspace");
        assertThat(projectCommand.getValue().initialStatus()).isEqualTo(ProjectStatus.ACTIVE);

        ArgumentCaptor<TaskCreationCommand> taskCommands =
                ArgumentCaptor.forClass(TaskCreationCommand.class);
        verify(taskCreationPort, org.mockito.Mockito.times(2)).createTask(taskCommands.capture());
        List<TaskCreationCommand> createdTasks = taskCommands.getAllValues();
        assertThat(createdTasks)
                .extracting(TaskCreationCommand::title)
                .containsExactly("Plan", "Ship");
        assertThat(createdTasks.get(0).dueAt()).isEqualTo(createdAt.plusSeconds(3600));
        assertThat(createdTasks.get(1).dueAt()).isNull();
        assertThat(createdTasks).allMatch(command -> command.projectId().equals(projectId));
        assertThat(createdTasks).allMatch(command -> command.creatorUserId().equals(actorId));
        assertThat(createdTasks).allMatch(command -> command.assigneeUserId() == null);
    }

    @Test
    void rejectsMoreThanFiftyTaskSnapshotsBeforePersistence() {
        List<ProjectTemplateDtos.TaskSnapshotRequest> tasks = new ArrayList<>();
        for (int index = 0; index < 51; index++) {
            tasks.add(
                    new ProjectTemplateDtos.TaskSnapshotRequest(
                            "Task " + index, null, ProjectTaskPriority.MEDIUM, null));
        }
        ProjectTemplateDtos.UpsertRequest request =
                new ProjectTemplateDtos.UpsertRequest(
                        "Template", "Project", null, ProjectStatus.PLANNING, tasks);

        assertThatThrownBy(() -> service().create(UUID.randomUUID(), request, jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50");

        verify(templateRepository, never()).save(any());
        verify(taskRepository, never()).saveAll(any());
        verify(currentActorService, never()).getRequiredActiveActor(any(), any());
    }

    private void stubTask(
            ProjectTemplateTask task, String title, ProjectTaskPriority priority, Long dueOffset) {
        when(task.getTitle()).thenReturn(title);
        when(task.getDescription()).thenReturn(null);
        when(task.getPriority()).thenReturn(priority);
        when(task.getDueOffsetMinutes()).thenReturn(dueOffset);
    }

    private ProjectTemplateService service() {
        return new ProjectTemplateService(
                templateRepository,
                taskRepository,
                currentActorService,
                projectCreationPort,
                taskCreationPort);
    }
}
