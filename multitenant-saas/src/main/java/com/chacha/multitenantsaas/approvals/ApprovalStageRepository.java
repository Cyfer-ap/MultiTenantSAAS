package com.chacha.multitenantsaas.approvals;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStageRepository extends JpaRepository<ApprovalStage, UUID> {
    List<ApprovalStage> findByTenantIdAndProjectIdAndDefinitionIdOrderByPositionIndexAsc(
            UUID tenantId, UUID projectId, UUID definitionId);

    void deleteByTenantIdAndProjectIdAndDefinitionId(
            UUID tenantId, UUID projectId, UUID definitionId);
}
