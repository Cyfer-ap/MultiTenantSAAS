package com.chacha.multitenantsaas.approvals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "approval_stage_reviewers")
public class ApprovalStageReviewer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Column(name = "reviewer_user_id", nullable = false)
    private UUID reviewerUserId;

    protected ApprovalStageReviewer() {}

    public ApprovalStageReviewer(
            UUID tenantId, UUID projectId, UUID definitionId, UUID stageId, UUID reviewerUserId) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.definitionId = definitionId;
        this.stageId = stageId;
        this.reviewerUserId = reviewerUserId;
    }

    public UUID getId() { return id; }
    public UUID getReviewerUserId() { return reviewerUserId; }
}
