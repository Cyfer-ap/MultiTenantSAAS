package com.chacha.multitenantsaas.approvals;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestStageExternalReviewerRepository
        extends JpaRepository<ApprovalRequestStageExternalReviewer, UUID> {

    boolean existsByTenantIdAndProjectIdAndRequestIdAndRequestStageIdAndExternalAccessGrantId(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            UUID requestStageId,
            UUID externalAccessGrantId);

    Optional<ApprovalRequestStageExternalReviewer>
            findByTenantIdAndProjectIdAndRequestIdAndRequestStageIdAndExternalAccessGrantId(
                    UUID tenantId,
                    UUID projectId,
                    UUID requestId,
                    UUID requestStageId,
                    UUID externalAccessGrantId);

    java.util.List<ApprovalRequestStageExternalReviewer>
            findByTenantIdAndProjectIdAndRequestIdAndRequestStageId(
                    UUID tenantId, UUID projectId, UUID requestId, UUID requestStageId);

    Page<ApprovalRequestStageExternalReviewer>
            findByTenantIdAndProjectIdAndExternalAccessGrantIdOrderByCreatedAtAsc(
                    UUID tenantId, UUID projectId, UUID externalAccessGrantId, Pageable pageable);
}
