package com.chacha.multitenantsaas.taskrelationships.entity;

import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "project_task_label_assignments",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_project_task_label_assignment",
                        columnNames = {"tenant_id", "project_id", "task_id", "label_id"}),
        indexes = {
            @Index(
                    name = "idx_project_task_label_assignment_task",
                    columnList = "tenant_id,project_id,task_id"),
            @Index(
                    name = "idx_project_task_label_assignment_label",
                    columnList = "tenant_id,project_id,label_id")
        })
public class ProjectTaskLabelAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private ProjectTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "label_id", nullable = false)
    private ProjectTaskLabel label;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    public ProjectTaskLabelAssignment() {}

    public ProjectTaskLabelAssignment(
            Tenant tenant, Project project, ProjectTask task, ProjectTaskLabel label) {
        this.tenant = tenant;
        this.project = project;
        this.task = task;
        this.label = label;
    }

    @PrePersist
    protected void onCreate() {
        this.assignedAt = Instant.now();
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

    public ProjectTask getTask() {
        return task;
    }

    public ProjectTaskLabel getLabel() {
        return label;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
