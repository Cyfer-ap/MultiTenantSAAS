package com.chacha.multitenantsaas.forms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "form_submissions",
        indexes =
                @Index(
                        name = "idx_form_submissions_history",
                        columnList = "tenant_id,project_id,form_id,submitted_at"))
public class FormSubmission {

    @Id private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(name = "definition_version", nullable = false)
    private int definitionVersion;

    @Column(name = "submitted_by_user_id", nullable = false)
    private UUID submittedByUserId;

    @Column(name = "payload_json", nullable = false, length = 12000)
    private String payloadJson;

    @Column(name = "created_task_id", nullable = false)
    private UUID createdTaskId;

    @Column(name = "validation_context", nullable = false, length = 1000)
    private String validationContext;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected FormSubmission() {}

    public FormSubmission(
            UUID id,
            UUID tenantId,
            UUID projectId,
            UUID formId,
            int definitionVersion,
            UUID submittedByUserId,
            String payloadJson,
            UUID createdTaskId,
            String validationContext) {
        this.id = id;
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.formId = formId;
        this.definitionVersion = definitionVersion;
        this.submittedByUserId = submittedByUserId;
        this.payloadJson = payloadJson;
        this.createdTaskId = createdTaskId;
        this.validationContext = validationContext;
    }

    @PrePersist
    void onCreate() {
        submittedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFormId() {
        return formId;
    }

    public int getDefinitionVersion() {
        return definitionVersion;
    }

    public UUID getSubmittedByUserId() {
        return submittedByUserId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public UUID getCreatedTaskId() {
        return createdTaskId;
    }

    public String getValidationContext() {
        return validationContext;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
