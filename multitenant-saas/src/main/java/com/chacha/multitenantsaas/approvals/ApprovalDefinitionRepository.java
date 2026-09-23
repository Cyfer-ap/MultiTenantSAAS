package com.chacha.multitenantsaas.approvals;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDefinitionRepository extends JpaRepository<ApprovalDefinition, UUID> {
    Page<ApprovalDefinition> findByTenantIdAndProjectIdOrderByNameAsc(
            UUID tenantId, UUID projectId, Pageable pageable);

    Optional<ApprovalDefinition> findByTenantIdAndProjectIdAndId(
            UUID tenantId, UUID projectId, UUID id);

    boolean existsByTenantIdAndProjectIdAndNormalizedName(
            UUID tenantId, UUID projectId, String normalizedName);

    boolean existsByTenantIdAndProjectIdAndNormalizedNameAndIdNot(
            UUID tenantId, UUID projectId, String normalizedName, UUID id);
}
