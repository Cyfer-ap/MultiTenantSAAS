package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository
        extends JpaRepository<Project, UUID> {

    Optional<Project> findByTenant_IdAndId(
            UUID tenantId,
            UUID projectId
    );

    List<Project> findAllByTenant_IdOrderByNameAsc(
            UUID tenantId
    );

    @Query("""
            SELECT project
            FROM Project project
            WHERE project.tenant.id = :tenantId
              AND (:status IS NULL OR project.status = :status)
              AND (
                    :search IS NULL
                    OR LOWER(project.name)
                        LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(project.description, ''))
                        LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Project> findTenantProjects(
            @Param("tenantId") UUID tenantId,
            @Param("status") ProjectStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    long countByTenant_Id(UUID tenantId);

    long countByTenant_IdAndStatus(
            UUID tenantId,
            ProjectStatus status
    );
}