package com.chacha.multitenantsaas.workflows;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    Page<WorkflowDefinition> findByTenantIdOrderByNameAsc(UUID tenantId, Pageable pageable);

    List<WorkflowDefinition> findByTenantIdAndStatusOrderByNameAsc(
            UUID tenantId, WorkflowStatus status);

    Optional<WorkflowDefinition> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndNormalizedName(UUID tenantId, String normalizedName);

    boolean existsByTenantIdAndNormalizedNameAndIdNot(
            UUID tenantId, String normalizedName, UUID id);
}
