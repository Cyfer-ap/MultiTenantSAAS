package com.chacha.multitenantsaas.taskrelationships.entity;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "project_task_labels",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_project_task_label_name",
                    columnNames = {"tenant_id", "project_id", "normalized_name"}),
            @UniqueConstraint(
                    name = "uk_project_task_label_scope_id",
                    columnNames = {"tenant_id", "project_id", "id"})
        },
        indexes =
                @Index(
                        name = "idx_project_task_labels_project",
                        columnList = "tenant_id,project_id,name"))
public class ProjectTaskLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 60)
    private String normalizedName;

    @Column(length = 7)
    private String color;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProjectTaskLabel() {}

    public ProjectTaskLabel(
            Tenant tenant, Project project, String name, String normalizedName, String color) {
        this.tenant = tenant;
        this.project = project;
        this.name = name;
        this.normalizedName = normalizedName;
        this.color = color;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getColor() {
        return color;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
