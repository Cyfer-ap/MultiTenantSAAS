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
        name = "task_dependencies",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_task_dependency_edge",
                        columnNames = {
                            "tenant_id", "project_id", "blocking_task_id", "dependent_task_id"
                        }),
        indexes = {
            @Index(
                    name = "idx_task_dependency_blocking",
                    columnList = "tenant_id,project_id,blocking_task_id"),
            @Index(
                    name = "idx_task_dependency_dependent",
                    columnList = "tenant_id,project_id,dependent_task_id")
        })
public class TaskDependency {

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
    @JoinColumn(name = "blocking_task_id", nullable = false)
    private ProjectTask blockingTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_task_id", nullable = false)
    private ProjectTask dependentTask;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public TaskDependency() {}

    public TaskDependency(
            Tenant tenant, Project project, ProjectTask blockingTask, ProjectTask dependentTask) {
        this.tenant = tenant;
        this.project = project;
        this.blockingTask = blockingTask;
        this.dependentTask = dependentTask;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
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

    public ProjectTask getBlockingTask() {
        return blockingTask;
    }

    public ProjectTask getDependentTask() {
        return dependentTask;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
