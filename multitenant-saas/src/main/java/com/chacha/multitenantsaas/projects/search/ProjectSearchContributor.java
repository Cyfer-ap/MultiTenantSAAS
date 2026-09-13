package com.chacha.multitenantsaas.projects.search;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.search.model.GlobalSearchResult;
import com.chacha.multitenantsaas.search.model.GlobalSearchResultType;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContext;
import com.chacha.multitenantsaas.search.spi.GlobalSearchContributor;
import com.chacha.multitenantsaas.search.spi.GlobalSearchHit;
import com.chacha.multitenantsaas.search.spi.GlobalSearchScoring;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ProjectSearchContributor implements GlobalSearchContributor {

    private static final int MAX_CANDIDATES = 100;

    private final ProjectRepository projectRepository;
    private final AuthorizationSecurityService authorizationSecurity;

    public ProjectSearchContributor(
            ProjectRepository projectRepository, AuthorizationSecurityService authorizationSecurity) {
        this.projectRepository = projectRepository;
        this.authorizationSecurity = authorizationSecurity;
    }

    @Override
    public List<GlobalSearchHit> search(
            GlobalSearchContext context, String normalizedQuery, int limit) {
        boolean tenantWide =
                authorizationSecurity.hasTenantPermission(
                        context.tenantId(), PlatformPermissionCodes.PROJECT_READ);

        Set<UUID> scopedProjectIds = projectScopedIds(context, PlatformPermissionCodes.PROJECT_READ);
        if (!tenantWide && scopedProjectIds.isEmpty()) {
            return List.of();
        }

        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(20, limit * 4));
        String pattern = "%" + normalizedQuery + "%";

        Specification<Project> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate =
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(
                                            root.get("tenant").get("id"), context.tenantId()),
                                    criteriaBuilder.notEqual(
                                            root.get("status"), ProjectStatus.ARCHIVED),
                                    criteriaBuilder.or(
                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(root.<String>get("name")),
                                                    pattern),
                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(
                                                            criteriaBuilder.coalesce(
                                                                    root.<String>get("description"),
                                                                    "")),
                                                    pattern)));

                    if (!tenantWide) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate, root.get("id").in(scopedProjectIds));
                    }
                    return predicate;
                };

        return projectRepository
                .findAll(
                        specification,
                        PageRequest.of(0, candidateLimit, Sort.by(Sort.Direction.ASC, "name")))
                .getContent()
                .stream()
                .filter(
                        project ->
                                authorizationSecurity.hasProjectPermission(
                                        context.tenantId(),
                                        project.getId(),
                                        PlatformPermissionCodes.PROJECT_READ))
                .map(project -> toHit(normalizedQuery, project))
                .toList();
    }

    private Set<UUID> projectScopedIds(GlobalSearchContext context, String permissionCode) {
        Set<UUID> projectIds = new LinkedHashSet<>();
        for (GlobalSearchContext.Grant grant : context.grants()) {
            if (grant.scopeType() == AuthorizationScopeType.PROJECT
                    && grant.scopeTargetId() != null
                    && grant.permissionCodes().contains(permissionCode)) {
                projectIds.add(grant.scopeTargetId());
            }
        }
        return projectIds;
    }

    private GlobalSearchHit toHit(String query, Project project) {
        GlobalSearchResult result =
                new GlobalSearchResult(
                        GlobalSearchResultType.PROJECT,
                        project.getId(),
                        null,
                        project.getName(),
                        project.getStatus().name());
        return new GlobalSearchHit(
                result,
                GlobalSearchScoring.score(
                        query, project.getName(), project.getDescription(), project.getStatus().name()));
    }
}
