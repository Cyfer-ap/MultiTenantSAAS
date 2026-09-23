package com.chacha.multitenantsaas.approvals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestStageRepository extends JpaRepository<ApprovalRequestStage, UUID> {
    List<ApprovalRequestStage> findByTenantIdAndProjectIdAndRequestIdOrderByPositionIndexAsc(
            UUID tenantId, UUID projectId, UUID requestId);

    Optional<ApprovalRequestStage> findByTenantIdAndProjectIdAndRequestIdAndPositionIndex(
            UUID tenantId, UUID projectId, UUID requestId, int positionIndex);
}
