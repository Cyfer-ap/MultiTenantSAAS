package com.chacha.multitenantsaas.tasks.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.projects.query.ProjectMembershipQueryService;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.search.model.GlobalSearchResultType;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContext;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class TaskSearchContributorTest {

    @Test
    @SuppressWarnings("unchecked")
    void projectMembershipCanConstrainAndAuthorizeTaskSearch() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ProjectTaskRepository repository = mock(ProjectTaskRepository.class);
        ProjectMembershipQueryService membershipQuery = mock(ProjectMembershipQueryService.class);
        AuthorizationSecurityService authorizationSecurity =
                mock(AuthorizationSecurityService.class);

        Project project = new Project();
        project.setId(projectId);
        project.setName("Phoenix");

        ProjectTask task = new ProjectTask();
        task.setId(UUID.randomUUID());
        task.setProject(project);
        task.setTitle("Prepare Phoenix launch");
        task.setDescription("Coordinate launch checklist");

        when(authorizationSecurity.hasTenantPermission(
                        tenantId, PlatformPermissionCodes.PROJECT_TASK_READ))
                .thenReturn(false);
        when(membershipQuery.findProjectIdsForUser(tenantId, userId)).thenReturn(Set.of(projectId));
        when(authorizationSecurity.canReadProjectTasks(
                        tenantId, projectId, PlatformPermissionCodes.PROJECT_TASK_READ))
                .thenReturn(true);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        var context = new GlobalSearchContext(tenantId, userId, List.of());
        var hits =
                new TaskSearchContributor(repository, membershipQuery, authorizationSecurity)
                        .search(context, "phoenix", 12);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().result().type()).isEqualTo(GlobalSearchResultType.TASK);
        assertThat(hits.getFirst().result().parentId()).isEqualTo(projectId);
    }
}
