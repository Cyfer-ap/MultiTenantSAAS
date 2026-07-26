package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "project_tasks",
        indexes = {
                @Index(
                        name = "idx_project_task_tenant",
                        columnList = "tenant_id"
                ),
                @Index(
                        name = "idx_project_task_project",
                        columnList = "project_id"
                ),
                @Index(
                        name = "idx_project_task_assignee",
                        columnList = "assignee_user_id"
                ),
                @Index(
                        name = "idx_project_task_project_status",
                        columnList = "project_id,status"
                ),
                @Index(
                        name = "idx_project_task_project_priority",
                        columnList = "project_id,priority"
                ),
                @Index(
                        name = "idx_project_task_due_at",
                        columnList = "due_at"
                )
        }
)
public class ProjectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tenant_id",
            nullable = false
    )
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "project_id",
            nullable = false
    )
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false
    )
    private AppUser createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_user_id")
    private AppUser assigneeUser;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectTaskStatus status =
            ProjectTaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectTaskPriority priority =
            ProjectTaskPriority.MEDIUM;

    private Instant dueAt;

    private Instant completedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProjectTask() {
    }

    public ProjectTask(
            Tenant tenant,
            Project project,
            AppUser createdByUser,
            AppUser assigneeUser,
            String title,
            String description,
            ProjectTaskPriority priority,
            Instant dueAt
    ) {
        this.tenant = tenant;
        this.project = project;
        this.createdByUser = createdByUser;
        this.assigneeUser = assigneeUser;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueAt = dueAt;
        this.status = ProjectTaskStatus.TODO;
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

    public AppUser getCreatedByUser() {
        return createdByUser;
    }

    public AppUser getAssigneeUser() {
        return assigneeUser;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ProjectTaskStatus getStatus() {
        return status;
    }

    public ProjectTaskPriority getPriority() {
        return priority;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setCreatedByUser(AppUser createdByUser) {
        this.createdByUser = createdByUser;
    }

    public void setAssigneeUser(AppUser assigneeUser) {
        this.assigneeUser = assigneeUser;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(ProjectTaskStatus status) {
        this.status = status;
    }

    public void setPriority(ProjectTaskPriority priority) {
        this.priority = priority;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}