package com.chacha.multitenantsaas.approvals;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    Optional<ApprovalRequest> findByTenantIdAndProjectIdAndId(
            UUID tenantId, UUID projectId, UUID id);

    Optional<ApprovalRequest> findByTenantIdAndWorkflowExecutionIdAndWorkflowNodeKey(
            UUID tenantId, UUID workflowExecutionId, String workflowNodeKey);

    Page<ApprovalRequest> findByTenantIdAndProjectIdOrderByCreatedAtDesc(
            UUID tenantId, UUID projectId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT request FROM ApprovalRequest request WHERE request.tenantId = :tenantId AND request.projectId = :projectId AND request.id = :requestId")
    Optional<ApprovalRequest> findForDecision(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("requestId") UUID requestId);
}
