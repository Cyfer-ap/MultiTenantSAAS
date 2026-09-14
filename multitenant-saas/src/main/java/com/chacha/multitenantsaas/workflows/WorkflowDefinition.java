package com.chacha.multitenantsaas.workflows;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "workflow_definitions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_workflow_definition_name",
                        columnNames = {"tenant_id", "normalized_name"}),
        indexes =
                @Index(
                        name = "idx_workflow_definitions_tenant_status_name",
                        columnList = "tenant_id,status,name"))
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status;

    @Column(name = "definition_version", nullable = false)
    private int definitionVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkflowDefinition() {}

    public WorkflowDefinition(
            UUID tenantId,
            UUID createdByUserId,
            String name,
            String normalizedName,
            String description) {
        this.tenantId = tenantId;
        this.createdByUserId = createdByUserId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        this.status = WorkflowStatus.DRAFT;
        this.definitionVersion = 1;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateDefinition(String name, String normalizedName, String description) {
        if (status == WorkflowStatus.ACTIVE) {
            throw new IllegalStateException("Pause an active workflow before editing it");
        }
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        this.definitionVersion++;
    }

    public void activate() {
        this.status = WorkflowStatus.ACTIVE;
    }

    public void pause() {
        if (status == WorkflowStatus.DRAFT) {
            throw new IllegalStateException("A draft workflow cannot be paused");
        }
        this.status = WorkflowStatus.PAUSED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public int getDefinitionVersion() {
        return definitionVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
