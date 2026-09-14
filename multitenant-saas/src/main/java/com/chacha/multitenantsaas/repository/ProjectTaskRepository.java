package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectTaskRepository
        extends JpaRepository<ProjectTask, UUID>, JpaSpecificationExecutor<ProjectTask> {

    Optional<ProjectTask> findByProject_Tenant_IdAndProject_IdAndId(
            UUID tenantId, UUID projectId, UUID taskId);

    Optional<ProjectTask> findByTenant_IdAndId(UUID tenantId, UUID taskId);

    @EntityGraph(attributePaths = {"project", "parentTask"})
    List<ProjectTask> findByProject_Tenant_IdAndProject_IdAndParentTask_IdOrderByCreatedAtAsc(
            UUID tenantId, UUID projectId, UUID parentTaskId, Pageable pageable);

    @EntityGraph(attributePaths = "project")
    Page<ProjectTask> findByTenant_IdAndAssigneeUser_IdAndStatusNotIn(
            UUID tenantId,
            UUID assigneeUserId,
            Collection<ProjectTaskStatus> excludedStatuses,
            Pageable pageable);

    @EntityGraph(attributePaths = "project")
    Page<ProjectTask> findByTenant_IdAndDueAtGreaterThanEqualAndDueAtLessThan(
            UUID tenantId, Instant from, Instant to, Pageable pageable);

    @EntityGraph(attributePaths = "project")
    Page<ProjectTask> findByTenant_IdAndProject_IdInAndDueAtGreaterThanEqualAndDueAtLessThan(
            UUID tenantId,
            Collection<UUID> projectIds,
            Instant from,
            Instant to,
            Pageable pageable);

    long countByTenant_Id(UUID tenantId);

    long countByTenant_IdAndStatus(UUID tenantId, ProjectTaskStatus status);

    long countByTenant_IdAndDueAtBeforeAndStatusNotIn(
            UUID tenantId, Instant currentTime, Collection<ProjectTaskStatus> excludedStatuses);

    default Page<ProjectTask> findProjectTasks(
            UUID tenantId,
            UUID projectId,
            ProjectTaskStatus status,
            ProjectTaskPriority priority,
            UUID assigneeUserId,
            String search,
            Pageable pageable) {
        Specification<ProjectTask> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate =
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(root.get("tenant").get("id"), tenantId),
                                    criteriaBuilder.equal(
                                            root.get("project").get("id"), projectId));

                    if (status != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.equal(root.get("status"), status));
                    }

                    if (priority != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.equal(root.get("priority"), priority));
                    }

                    if (assigneeUserId != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.equal(
                                                root.get("assigneeUser").get("id"),
                                                assigneeUserId));
                    }

                    if (search != null) {
                        String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.or(
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                root.<String>get("title")),
                                                        pattern),
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                criteriaBuilder.coalesce(
                                                                        root.<String>get(
                                                                                "description"),
                                                                        "")),
                                                        pattern)));
                    }

                    return predicate;
                };

        return findAll(specification, pageable);
    }
}
