package com.chacha.multitenantsaas.approvals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "approval_request_stage_reviewers")
public class ApprovalRequestStageReviewer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "request_stage_id", nullable = false)
    private UUID requestStageId;

    @Column(name = "reviewer_user_id", nullable = false)
    private UUID reviewerUserId;

    protected ApprovalRequestStageReviewer() {}

    public ApprovalRequestStageReviewer(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            UUID requestStageId,
            UUID reviewerUserId) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.requestId = requestId;
        this.requestStageId = requestStageId;
        this.reviewerUserId = reviewerUserId;
    }

    public UUID getReviewerUserId() {
        return reviewerUserId;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getRequestStageId() {
        return requestStageId;
    }
}
