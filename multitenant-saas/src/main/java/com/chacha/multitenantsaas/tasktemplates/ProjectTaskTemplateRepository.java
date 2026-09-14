package com.chacha.multitenantsaas.tasktemplates;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTaskTemplateRepository extends JpaRepository<ProjectTaskTemplate, UUID> {

    Page<ProjectTaskTemplate> findByTenantIdAndProjectIdOrderByNameAsc(
            UUID tenantId, UUID projectId, Pageable pageable);

    Optional<ProjectTaskTemplate> findByTenantIdAndProjectIdAndId(
            UUID tenantId, UUID projectId, UUID templateId);

    boolean existsByTenantIdAndProjectIdAndNormalizedName(
            UUID tenantId, UUID projectId, String normalizedName);

    boolean existsByTenantIdAndProjectIdAndNormalizedNameAndIdNot(
            UUID tenantId, UUID projectId, String normalizedName, UUID id);

    long countByTenantIdAndProjectId(UUID tenantId, UUID projectId);
}
