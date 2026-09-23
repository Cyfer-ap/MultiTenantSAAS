package com.chacha.multitenantsaas.approvals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_request_stage_external_reviewers")
public class ApprovalRequestStageExternalReviewer {

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

    @Column(name = "external_access_grant_id", nullable = false)
    private UUID externalAccessGrantId;

    @Column(name = "guest_name_snapshot", nullable = false, length = 150)
    private String guestNameSnapshot;

    @Column(name = "guest_email_snapshot", nullable = false, length = 150)
    private String guestEmailSnapshot;

    @Column(name = "assigned_by_user_id", nullable = false)
    private UUID assignedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ApprovalRequestStageExternalReviewer() {}

    public ApprovalRequestStageExternalReviewer(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            UUID requestStageId,
            ApprovalExternalGrantSnapshot grant,
            UUID assignedByUserId) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.requestId = requestId;
        this.requestStageId = requestStageId;
        this.externalAccessGrantId = grant.grantId();
        this.guestNameSnapshot = grant.guestName();
        this.guestEmailSnapshot = grant.guestEmail();
        this.assignedByUserId = assignedByUserId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getRequestStageId() {
        return requestStageId;
    }

    public UUID getExternalAccessGrantId() {
        return externalAccessGrantId;
    }

    public String getGuestNameSnapshot() {
        return guestNameSnapshot;
    }

    public String getGuestEmailSnapshot() {
        return guestEmailSnapshot;
    }

    public UUID getAssignedByUserId() {
        return assignedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
