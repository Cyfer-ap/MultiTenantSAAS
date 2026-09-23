package com.chacha.multitenantsaas.approvals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "definition_version", nullable = false)
    private int definitionVersion;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "workflow_version", nullable = false)
    private int workflowVersion;

    @Column(name = "workflow_execution_id", nullable = false)
    private UUID workflowExecutionId;

    @Column(name = "workflow_node_key", nullable = false, length = 64)
    private String workflowNodeKey;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalRequestStatus status;

    @Column(name = "current_stage_index", nullable = false)
    private int currentStageIndex;

    @Column(name = "approved_next_node_key", length = 64)
    private String approvedNextNodeKey;

    @Column(name = "rejected_next_node_key", length = 64)
    private String rejectedNextNodeKey;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ApprovalRequest() {}

    public ApprovalRequest(ApprovalCheckpointCommand command, int definitionVersion) {
        tenantId = command.tenantId();
        projectId = command.projectId();
        definitionId = command.approvalDefinitionId();
        this.definitionVersion = definitionVersion;
        workflowId = command.workflowId();
        workflowVersion = command.workflowVersion();
        workflowExecutionId = command.workflowExecutionId();
        workflowNodeKey = command.workflowNodeKey();
        taskId = command.taskId();
        actorUserId = command.actorUserId();
        status = ApprovalRequestStatus.PENDING;
        currentStageIndex = 0;
        approvedNextNodeKey = command.approvedNextNodeKey();
        rejectedNextNodeKey = command.rejectedNextNodeKey();
        createdAt = Instant.now();
    }

    public void advance() {
        currentStageIndex++;
    }

    public void approve() {
        status = ApprovalRequestStatus.APPROVED;
        completedAt = Instant.now();
    }

    public void reject() {
        status = ApprovalRequestStatus.REJECTED;
        completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getDefinitionId() {
        return definitionId;
    }

    public int getDefinitionVersion() {
        return definitionVersion;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public int getWorkflowVersion() {
        return workflowVersion;
    }

    public UUID getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public String getWorkflowNodeKey() {
        return workflowNodeKey;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public ApprovalRequestStatus getStatus() {
        return status;
    }

    public int getCurrentStageIndex() {
        return currentStageIndex;
    }

    public String getApprovedNextNodeKey() {
        return approvedNextNodeKey;
    }

    public String getRejectedNextNodeKey() {
        return rejectedNextNodeKey;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
