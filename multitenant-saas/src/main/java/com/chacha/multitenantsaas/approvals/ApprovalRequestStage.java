package com.chacha.multitenantsaas.approvals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_request_stages")
public class ApprovalRequestStage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "stage_key", nullable = false, length = 64)
    private String stageKey;

    @Column(name = "stage_name", nullable = false, length = 120)
    private String stageName;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @Column(name = "allow_requester_approval", nullable = false)
    private boolean allowRequesterApproval;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStageStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_actor_type", length = 30)
    private ApprovalDecisionActorType decisionActorType;

    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;

    @Column(name = "external_decided_by_grant_id")
    private UUID externalDecidedByGrantId;

    @Column(name = "external_decided_by_name", length = 150)
    private String externalDecidedByName;

    @Column(name = "external_decided_by_email", length = 150)
    private String externalDecidedByEmail;

    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ApprovalRequestStage() {}

    public ApprovalRequestStage(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            String stageKey,
            String stageName,
            int positionIndex,
            boolean allowRequesterApproval,
            ApprovalStageStatus status) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.requestId = requestId;
        this.stageKey = stageKey;
        this.stageName = stageName;
        this.positionIndex = positionIndex;
        this.allowRequesterApproval = allowRequesterApproval;
        this.status = status;
    }

    public void activate() {
        status = ApprovalStageStatus.PENDING;
    }

    public void decide(UUID actorUserId, ApprovalDecisionOutcome outcome, String comment) {
        requirePending();
        applyOutcome(outcome, comment);
        decisionActorType = ApprovalDecisionActorType.TENANT_USER;
        decidedByUserId = actorUserId;
        externalDecidedByGrantId = null;
        externalDecidedByName = null;
        externalDecidedByEmail = null;
    }

    public void decideExternal(
            UUID grantId,
            String guestName,
            String guestEmail,
            ApprovalDecisionOutcome outcome,
            String comment) {
        requirePending();
        if (guestName == null || guestName.isBlank() || guestEmail == null || guestEmail.isBlank()) {
            throw new IllegalArgumentException("External approval identity is required");
        }
        applyOutcome(outcome, comment);
        decisionActorType = ApprovalDecisionActorType.EXTERNAL_GUEST;
        decidedByUserId = null;
        externalDecidedByGrantId = grantId;
        externalDecidedByName = guestName.trim();
        externalDecidedByEmail = guestEmail.trim();
    }

    private void requirePending() {
        if (status != ApprovalStageStatus.PENDING) {
            throw new IllegalArgumentException("Approval stage is no longer pending");
        }
    }

    private void applyOutcome(ApprovalDecisionOutcome outcome, String comment) {
        status =
                outcome == ApprovalDecisionOutcome.APPROVE
                        ? ApprovalStageStatus.APPROVED
                        : ApprovalStageStatus.REJECTED;
        decisionComment = comment == null || comment.isBlank() ? null : comment.trim();
        decidedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public String getStageKey() {
        return stageKey;
    }

    public String getStageName() {
        return stageName;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public boolean isAllowRequesterApproval() {
        return allowRequesterApproval;
    }

    public ApprovalStageStatus getStatus() {
        return status;
    }

    public ApprovalDecisionActorType getDecisionActorType() {
        return decisionActorType;
    }

    public UUID getDecidedByUserId() {
        return decidedByUserId;
    }

    public UUID getExternalDecidedByGrantId() {
        return externalDecidedByGrantId;
    }

    public String getExternalDecidedByName() {
        return externalDecidedByName;
    }

    public String getExternalDecidedByEmail() {
        return externalDecidedByEmail;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
