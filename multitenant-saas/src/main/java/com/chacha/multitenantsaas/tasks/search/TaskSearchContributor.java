package com.chacha.multitenantsaas.tasks.search;

import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.projects.query.ProjectMembershipQueryService;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
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
public class TaskSearchContributor implements GlobalSearchContributor {

    private static final int MAX_CANDIDATES = 100;

    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectMembershipQueryService projectMembershipQueryService;
    private final AuthorizationSecurityService authorizationSecurity;

    public TaskSearchContributor(
            ProjectTaskRepository projectTaskRepository,
            ProjectMembershipQueryService projectMembershipQueryService,
            AuthorizationSecurityService authorizationSecurity) {
        this.projectTaskRepository = projectTaskRepository;
        this.projectMembershipQueryService = projectMembershipQueryService;
        this.authorizationSecurity = authorizationSecurity;
    }

    @Override
    public List<GlobalSearchHit> search(
            GlobalSearchContext context, String normalizedQuery, int limit) {
        boolean tenantWide =
                authorizationSecurity.hasTenantPermission(
                        context.tenantId(), PlatformPermissionCodes.PROJECT_TASK_READ);

        Set<UUID> readableProjectIds =
                projectScopedIds(context, PlatformPermissionCodes.PROJECT_TASK_READ);
        readableProjectIds.addAll(
                projectMembershipQueryService.findProjectIdsForUser(
                        context.tenantId(), context.userId()));

        if (!tenantWide && readableProjectIds.isEmpty()) {
            return List.of();
        }

        int candidateLimit = Math.min(MAX_CANDIDATES, Math.max(20, limit * 4));
        String pattern = "%" + normalizedQuery + "%";

        Specification<ProjectTask> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate =
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(
                                            root.get("tenant").get("id"), context.tenantId()),
                                    criteriaBuilder.notEqual(
                                            root.get("project").get("status"),
                                            ProjectStatus.ARCHIVED),
                                    criteriaBuilder.or(
                                            criteriaBuilder.like(
                                                    criteriaBuilder.lower(
                                                            root.<String>get("title")),
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
                                        predicate,
                                        root.get("project").get("id").in(readableProjectIds));
                    }
                    return predicate;
                };

        return projectTaskRepository
                .findAll(
                        specification,
                        PageRequest.of(0, candidateLimit, Sort.by(Sort.Direction.ASC, "title")))
                .getContent()
                .stream()
                .filter(
                        task ->
                                authorizationSecurity.canReadProjectTasks(
                                        context.tenantId(),
                                        task.getProject().getId(),
                                        PlatformPermissionCodes.PROJECT_TASK_READ))
                .map(task -> toHit(normalizedQuery, task))
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

    private GlobalSearchHit toHit(String query, ProjectTask task) {
        GlobalSearchResult result =
                new GlobalSearchResult(
                        GlobalSearchResultType.TASK,
                        task.getId(),
                        task.getProject().getId(),
                        task.getTitle(),
                        task.getProject().getName());
        return new GlobalSearchHit(
                result,
                GlobalSearchScoring.score(
                        query,
                        task.getTitle(),
                        task.getDescription(),
                        task.getProject().getName()));
    }
}
