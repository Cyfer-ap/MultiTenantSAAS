package com.chacha.multitenantsaas.approvals;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStageReviewerRepository extends JpaRepository<ApprovalStageReviewer, UUID> {
    List<ApprovalStageReviewer> findByTenantIdAndProjectIdAndDefinitionIdAndStageIdOrderByReviewerUserIdAsc(
            UUID tenantId, UUID projectId, UUID definitionId, UUID stageId);

    void deleteByTenantIdAndProjectIdAndDefinitionId(
            UUID tenantId, UUID projectId, UUID definitionId);
}
