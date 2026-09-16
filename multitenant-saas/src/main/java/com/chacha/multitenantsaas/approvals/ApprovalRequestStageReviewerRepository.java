package com.chacha.multitenantsaas.approvals;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRequestStageReviewerRepository
        extends JpaRepository<ApprovalRequestStageReviewer, UUID> {

    List<ApprovalRequestStageReviewer> findByTenantIdAndProjectIdAndRequestIdAndRequestStageIdOrderByReviewerUserIdAsc(
            UUID tenantId, UUID projectId, UUID requestId, UUID requestStageId);

    boolean existsByTenantIdAndProjectIdAndRequestIdAndRequestStageIdAndReviewerUserId(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            UUID requestStageId,
            UUID reviewerUserId);

    @Query("""
            SELECT reviewer
            FROM ApprovalRequestStageReviewer reviewer, ApprovalRequestStage stage, ApprovalRequest request
            WHERE reviewer.tenantId = :tenantId
              AND reviewer.projectId = :projectId
              AND reviewer.reviewerUserId = :reviewerUserId
              AND stage.id = reviewer.requestStageId
              AND stage.requestId = reviewer.requestId
              AND stage.status = com.chacha.multitenantsaas.approvals.ApprovalStageStatus.PENDING
              AND request.id = reviewer.requestId
              AND request.status = com.chacha.multitenantsaas.approvals.ApprovalRequestStatus.PENDING
            ORDER BY request.createdAt DESC
            """)
    Page<ApprovalRequestStageReviewer> findPendingInbox(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("reviewerUserId") UUID reviewerUserId,
            Pageable pageable);
}
