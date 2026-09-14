package com.chacha.multitenantsaas.projecttemplates;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, UUID> {

    Page<ProjectTemplate> findByTenantIdOrderByNameAsc(UUID tenantId, Pageable pageable);

    Optional<ProjectTemplate> findByTenantIdAndId(UUID tenantId, UUID id);

    long countByTenantId(UUID tenantId);

    boolean existsByTenantIdAndNormalizedName(UUID tenantId, String normalizedName);

    boolean existsByTenantIdAndNormalizedNameAndIdNot(
            UUID tenantId, String normalizedName, UUID id);
}
