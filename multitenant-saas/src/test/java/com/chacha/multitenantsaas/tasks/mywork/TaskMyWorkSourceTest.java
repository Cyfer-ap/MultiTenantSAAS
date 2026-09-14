package com.chacha.multitenantsaas.tasks.mywork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class TaskMyWorkSourceTest {

    @Test
    void returnsOnlyAssignedTasksThatRemainReadable() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID readableProjectId = UUID.randomUUID();
        UUID hiddenProjectId = UUID.randomUUID();
        ProjectTaskRepository repository = mock(ProjectTaskRepository.class);
        AuthorizationSecurityService authorizationSecurity =
                mock(AuthorizationSecurityService.class);
        ProjectTask readable = task(readableProjectId, "Readable");
        ProjectTask hidden = task(hiddenProjectId, "Hidden");

        when(repository.findByTenant_IdAndAssigneeUser_IdAndStatusNotIn(
                        eq(tenantId), eq(userId), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(readable, hidden)));
        when(authorizationSecurity.canReadProjectTasks(
                        tenantId, readableProjectId, PlatformPermissionCodes.PROJECT_TASK_READ))
                .thenReturn(true);
        when(authorizationSecurity.canReadProjectTasks(
                        tenantId, hiddenProjectId, PlatformPermissionCodes.PROJECT_TASK_READ))
                .thenReturn(false);

        var results =
                new TaskMyWorkSource(repository, authorizationSecurity)
                        .findAssignedOpenTasks(tenantId, userId, 50);

        assertThat(results)
                .singleElement()
                .satisfies(item -> assertThat(item.title()).isEqualTo("Readable"));
        verify(authorizationSecurity)
                .canReadProjectTasks(
                        tenantId, readableProjectId, PlatformPermissionCodes.PROJECT_TASK_READ);
        verify(authorizationSecurity)
                .canReadProjectTasks(
                        tenantId, hiddenProjectId, PlatformPermissionCodes.PROJECT_TASK_READ);
    }

    private ProjectTask task(UUID projectId, String title) {
        Project project = mock(Project.class);
        ProjectTask task = mock(ProjectTask.class);
        when(project.getId()).thenReturn(projectId);
        when(project.getName()).thenReturn("Project " + title);
        when(task.getId()).thenReturn(UUID.randomUUID());
        when(task.getProject()).thenReturn(project);
        when(task.getTitle()).thenReturn(title);
        when(task.getStatus()).thenReturn(ProjectTaskStatus.TODO);
        when(task.getPriority()).thenReturn(ProjectTaskPriority.MEDIUM);
        when(task.getDueAt()).thenReturn(null);
        when(task.getUpdatedAt()).thenReturn(Instant.now());
        return task;
    }
}
