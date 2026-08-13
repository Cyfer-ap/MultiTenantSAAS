package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.ProjectMember;
import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, UUID>, JpaSpecificationExecutor<ProjectMember> {

    boolean existsByProject_IdAndUser_Id(UUID projectId, UUID userId);

    Optional<ProjectMember> findByProject_Tenant_IdAndProject_IdAndUser_Id(
            UUID tenantId, UUID projectId, UUID userId);

    long countByProject_IdAndRole(UUID projectId, ProjectMemberRole role);

    boolean existsByProject_Tenant_IdAndProject_IdAndUser_Id(
            UUID tenantId, UUID projectId, UUID userId);

    long countByProject_Tenant_Id(UUID tenantId);

    default Page<ProjectMember> findProjectMembers(
            UUID tenantId,
            UUID projectId,
            ProjectMemberRole role,
            String search,
            Pageable pageable) {
        Specification<ProjectMember> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate =
                            criteriaBuilder.and(
                                    criteriaBuilder.equal(
                                            root.get("project").get("tenant").get("id"), tenantId),
                                    criteriaBuilder.equal(
                                            root.get("project").get("id"), projectId));

                    if (role != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate, criteriaBuilder.equal(root.get("role"), role));
                    }

                    if (search != null) {
                        String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.or(
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                root.get("user")
                                                                        .<String>get("fullName")),
                                                        pattern),
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                root.get("user")
                                                                        .<String>get("email")),
                                                        pattern)));
                    }

                    return predicate;
                };

        return findAll(specification, pageable);
    }
}
