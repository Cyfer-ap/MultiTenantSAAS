package com.chacha.multitenantsaas.projects.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.search.model.GlobalSearchResultType;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContext;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class ProjectSearchContributorTest {

    @Test
    @SuppressWarnings("unchecked")
    void returnsOnlyRuntimeAuthorizedProjectHits() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ProjectRepository repository = mock(ProjectRepository.class);
        AuthorizationSecurityService authorizationSecurity = mock(AuthorizationSecurityService.class);

        Project project = new Project();
        project.setId(projectId);
        project.setName("Phoenix rollout");
        project.setDescription("Launch project");
        project.setStatus(ProjectStatus.ACTIVE);

        when(authorizationSecurity.hasTenantPermission(
                        tenantId, PlatformPermissionCodes.PROJECT_READ))
                .thenReturn(false);
        when(authorizationSecurity.hasProjectPermission(
                        tenantId, projectId, PlatformPermissionCodes.PROJECT_READ))
                .thenReturn(true);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(project)));

        var context =
                new GlobalSearchContext(
                        tenantId,
                        userId,
                        List.of(
                                new GlobalSearchContext.Grant(
                                        AuthorizationScopeType.PROJECT,
                                        projectId,
                                        List.of(PlatformPermissionCodes.PROJECT_READ))));

        var hits = new ProjectSearchContributor(repository, authorizationSecurity).search(context, "phoenix", 12);

        assertThat(hits).hasSize(1);
        assertThat(hits.getFirst().result().type()).isEqualTo(GlobalSearchResultType.PROJECT);
        assertThat(hits.getFirst().result().id()).isEqualTo(projectId);
    }

    @Test
    void doesNotQueryProjectsWhenCallerHasNoSearchableProjectScope() {
        UUID tenantId = UUID.randomUUID();
        ProjectRepository repository = mock(ProjectRepository.class);
        AuthorizationSecurityService authorizationSecurity = mock(AuthorizationSecurityService.class);
        when(authorizationSecurity.hasTenantPermission(
                        tenantId, PlatformPermissionCodes.PROJECT_READ))
                .thenReturn(false);

        var context = new GlobalSearchContext(tenantId, UUID.randomUUID(), List.of());

        var hits = new ProjectSearchContributor(repository, authorizationSecurity).search(context, "phoenix", 12);

        assertThat(hits).isEmpty();
        verifyNoInteractions(repository);
    }
}
