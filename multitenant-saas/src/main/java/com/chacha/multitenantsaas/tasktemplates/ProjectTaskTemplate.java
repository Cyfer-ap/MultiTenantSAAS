package com.chacha.multitenantsaas.tasktemplates;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
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
        name = "project_task_templates",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_project_task_template_name",
                        columnNames = {"tenant_id", "project_id", "normalized_name"}),
        indexes =
                @Index(
                        name = "idx_project_task_templates_project",
                        columnList = "tenant_id,project_id,name"))
public class ProjectTaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 80)
    private String normalizedName;

    @Column(name = "task_title", nullable = false, length = 200)
    private String taskTitle;

    @Column(name = "task_description", length = 4000)
    private String taskDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectTaskPriority priority;

    @Column(name = "due_offset_minutes")
    private Long dueOffsetMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectTaskTemplate() {}

    public ProjectTaskTemplate(
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            UUID assigneeUserId,
            String name,
            String normalizedName,
            String taskTitle,
            String taskDescription,
            ProjectTaskPriority priority,
            Long dueOffsetMinutes) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.createdByUserId = createdByUserId;
        this.assigneeUserId = assigneeUserId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.priority = priority;
        this.dueOffsetMinutes = dueOffsetMinutes;
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
            UUID assigneeUserId,
            String name,
            String normalizedName,
            String taskTitle,
            String taskDescription,
            ProjectTaskPriority priority,
            Long dueOffsetMinutes) {
        this.assigneeUserId = assigneeUserId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.priority = priority;
        this.dueOffsetMinutes = dueOffsetMinutes;
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

    public UUID getAssigneeUserId() {
        return assigneeUserId;
    }

    public String getName() {
        return name;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public ProjectTaskPriority getPriority() {
        return priority;
    }

    public Long getDueOffsetMinutes() {
        return dueOffsetMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
