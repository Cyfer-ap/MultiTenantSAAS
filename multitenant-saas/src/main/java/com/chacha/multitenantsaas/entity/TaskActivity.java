package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "task_activities",
        indexes = {
            @Index(
                    name = "idx_task_activity_task_created",
                    columnList = "tenant_id,project_id,task_id,created_at")
        })
public class TaskActivity {

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
    @JoinColumn(name = "actor_user_id", nullable = false)
    private AppUser actorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 40)
    private TaskActivityType activityType;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public TaskActivity() {}

    public TaskActivity(
            Tenant tenant,
            Project project,
            ProjectTask task,
            AppUser actorUser,
            TaskActivityType activityType,
            String summary) {
        this.tenant = tenant;
        this.project = project;
        this.task = task;
        this.actorUser = actorUser;
        this.activityType = activityType;
        this.summary = summary;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
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

    public AppUser getActorUser() {
        return actorUser;
    }

    public TaskActivityType getActivityType() {
        return activityType;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
