package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectRepository
        extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    Optional<Project> findByTenant_IdAndId(UUID tenantId, UUID projectId);

    List<Project> findAllByTenant_IdOrderByNameAsc(UUID tenantId);

    default Page<Project> findTenantProjects(
            UUID tenantId, ProjectStatus status, String search, Pageable pageable) {
        Specification<Project> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate = criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);

                    if (status != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.equal(root.get("status"), status));
                    }

                    if (search != null) {
                        String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.or(
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                root.<String>get("name")),
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

    long countByTenant_Id(UUID tenantId);

    long countByTenant_IdAndStatus(UUID tenantId, ProjectStatus status);
}
