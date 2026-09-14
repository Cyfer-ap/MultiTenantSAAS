package com.chacha.multitenantsaas.tasks.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.calendar.spi.CalendarDeadlineContext;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.projects.query.ProjectMembershipQueryService;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class TaskCalendarDeadlineSourceTest {

    @Test
    void projectMembershipConstrainsCandidatesAndStillRevalidatesTaskReadAccess() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        Instant dueAt = Instant.parse("2026-09-18T10:30:00Z");

        ProjectTaskRepository repository = mock(ProjectTaskRepository.class);
        ProjectMembershipQueryService membershipQuery = mock(ProjectMembershipQueryService.class);
        AuthorizationSecurityService authorizationSecurity =
                mock(AuthorizationSecurityService.class);

        Project project = new Project();
        project.setId(projectId);
        project.setName("Phoenix");

        ProjectTask task = new ProjectTask();
        task.setId(taskId);
        task.setProject(project);
        task.setTitle("Ship calendar view");
        task.setStatus(ProjectTaskStatus.IN_PROGRESS);
        task.setPriority(ProjectTaskPriority.HIGH);
        task.setDueAt(dueAt);

        when(authorizationSecurity.hasTenantPermission(
                        tenantId, PlatformPermissionCodes.PROJECT_TASK_READ))
                .thenReturn(false);
        when(membershipQuery.findProjectIdsForUser(tenantId, userId)).thenReturn(Set.of(projectId));
        when(repository.findByTenant_IdAndProject_IdInAndDueAtGreaterThanEqualAndDueAtLessThan(
                        eq(tenantId), eq(Set.of(projectId)), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(authorizationSecurity.canReadProjectTasks(
                        tenantId, projectId, PlatformPermissionCodes.PROJECT_TASK_READ))
                .thenReturn(true);

        var source =
                new TaskCalendarDeadlineSource(repository, membershipQuery, authorizationSecurity);
        var context = new CalendarDeadlineContext(tenantId, userId, List.of());

        var deadlines = source.findDeadlines(context, from, to, 25);

        assertThat(deadlines).hasSize(1);
        assertThat(deadlines.getFirst().taskId()).isEqualTo(taskId);
        assertThat(deadlines.getFirst().projectId()).isEqualTo(projectId);
        assertThat(deadlines.getFirst().projectName()).isEqualTo("Phoenix");
        assertThat(deadlines.getFirst().dueAt()).isEqualTo(dueAt);
    }

    @Test
    void noReadableProjectScopeAvoidsQueryingTaskCandidates() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectTaskRepository repository = mock(ProjectTaskRepository.class);
        ProjectMembershipQueryService membershipQuery = mock(ProjectMembershipQueryService.class);
        AuthorizationSecurityService authorizationSecurity =
                mock(AuthorizationSecurityService.class);

        when(authorizationSecurity.hasTenantPermission(
                        tenantId, PlatformPermissionCodes.PROJECT_TASK_READ))
                .thenReturn(false);
        when(membershipQuery.findProjectIdsForUser(tenantId, userId)).thenReturn(Set.of());

        var source =
                new TaskCalendarDeadlineSource(repository, membershipQuery, authorizationSecurity);
        var context = new CalendarDeadlineContext(tenantId, userId, List.of());

        var deadlines =
                source.findDeadlines(
                        context,
                        Instant.parse("2026-09-01T00:00:00Z"),
                        Instant.parse("2026-10-01T00:00:00Z"),
                        25);

        assertThat(deadlines).isEmpty();
        verifyNoInteractions(repository);
    }
}
