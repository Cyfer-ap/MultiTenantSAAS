package com.chacha.multitenantsaas.projecttemplates;

import com.chacha.multitenantsaas.entity.ProjectStatus;
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
        name = "project_templates",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_project_template_name",
                        columnNames = {"tenant_id", "normalized_name"}),
        indexes = @Index(name = "idx_project_templates_tenant_name", columnList = "tenant_id,name"))
public class ProjectTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 80)
    private String normalizedName;

    @Column(name = "project_name_seed", nullable = false, length = 150)
    private String projectNameSeed;

    @Column(name = "project_description", length = 2000)
    private String projectDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "initial_status", nullable = false, length = 30)
    private ProjectStatus initialStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectTemplate() {}

    public ProjectTemplate(
            UUID tenantId,
            UUID createdByUserId,
            String name,
            String normalizedName,
            String projectNameSeed,
            String projectDescription,
            ProjectStatus initialStatus) {
        this.tenantId = tenantId;
        this.createdByUserId = createdByUserId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.projectNameSeed = projectNameSeed;
        this.projectDescription = projectDescription;
        this.initialStatus = initialStatus;
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

    public void update(
            String name,
            String normalizedName,
            String projectNameSeed,
            String projectDescription,
            ProjectStatus initialStatus) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.projectNameSeed = projectNameSeed;
        this.projectDescription = projectDescription;
        this.initialStatus = initialStatus;
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

    public String getProjectNameSeed() {
        return projectNameSeed;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public ProjectStatus getInitialStatus() {
        return initialStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
