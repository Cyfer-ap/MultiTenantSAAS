package com.chacha.multitenantsaas.approvals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_definitions")
public class ApprovalDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

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
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    @Column(name = "definition_version", nullable = false)
    private int definitionVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApprovalDefinition() {}

    public ApprovalDefinition(
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            String name,
            String normalizedName,
            String description) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.createdByUserId = createdByUserId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
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

    public void update(String name, String normalizedName, String description) {
        if (status == ApprovalStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Active approval definitions must be paused before editing");
        }
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        definitionVersion++;
        updatedAt = Instant.now();
    }

    public void activate() {
        status = ApprovalStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void pause() {
        if (status != ApprovalStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active approval definitions can be paused");
        }
        status = ApprovalStatus.PAUSED;
        updatedAt = Instant.now();
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

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getDescription() {
        return description;
    }

    public ApprovalStatus getStatus() {
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
