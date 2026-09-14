package com.chacha.multitenantsaas.workflows;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "workflow_executions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_workflow_execution_event",
                        columnNames = {"tenant_id", "workflow_id", "event_key"}),
        indexes = {
            @Index(
                    name = "idx_workflow_executions_history",
                    columnList = "tenant_id,workflow_id,started_at"),
            @Index(
                    name = "idx_workflow_executions_tenant_started",
                    columnList = "tenant_id,started_at")
        })
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "workflow_version", nullable = false)
    private int workflowVersion;

    @Column(name = "event_key", nullable = false, length = 200)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_operation", nullable = false, length = 80)
    private WorkflowOperation triggerOperation;

    @Column(name = "source_entity_type", length = 50)
    private String sourceEntityType;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowExecutionStatus status;

    @Column(length = 2000)
    private String explanation;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected WorkflowExecution() {}

    public WorkflowExecution(
            UUID tenantId,
            UUID workflowId,
            int workflowVersion,
            String eventKey,
            WorkflowOperation triggerOperation,
            String sourceEntityType,
            UUID sourceEntityId) {
        this.tenantId = tenantId;
        this.workflowId = workflowId;
        this.workflowVersion = workflowVersion;
        this.eventKey = eventKey;
        this.triggerOperation = triggerOperation;
        this.sourceEntityType = sourceEntityType;
        this.sourceEntityId = sourceEntityId;
        this.status = WorkflowExecutionStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void succeed(String explanation) {
        this.status = WorkflowExecutionStatus.SUCCEEDED;
        this.explanation = truncate(explanation);
        this.errorMessage = null;
        this.completedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = WorkflowExecutionStatus.FAILED;
        this.errorMessage = truncate(errorMessage);
        this.completedAt = Instant.now();
    }

    public void skip(String explanation) {
        this.status = WorkflowExecutionStatus.SKIPPED;
        this.explanation = truncate(explanation);
        this.errorMessage = null;
        this.completedAt = Instant.now();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public int getWorkflowVersion() {
        return workflowVersion;
    }

    public String getEventKey() {
        return eventKey;
    }

    public WorkflowOperation getTriggerOperation() {
        return triggerOperation;
    }

    public String getSourceEntityType() {
        return sourceEntityType;
    }

    public UUID getSourceEntityId() {
        return sourceEntityId;
    }

    public WorkflowExecutionStatus getStatus() {
        return status;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
