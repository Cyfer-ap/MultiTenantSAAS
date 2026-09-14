package com.chacha.multitenantsaas.projecttemplates;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "project_template_tasks",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_project_template_task_position",
                        columnNames = {"tenant_id", "template_id", "position_index"}),
        indexes =
                @Index(
                        name = "idx_project_template_tasks_template",
                        columnList = "tenant_id,template_id,position_index"))
public class ProjectTemplateTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectTaskPriority priority;

    @Column(name = "due_offset_minutes")
    private Long dueOffsetMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProjectTemplateTask() {}

    public ProjectTemplateTask(
            UUID tenantId,
            UUID templateId,
            int positionIndex,
            String title,
            String description,
            ProjectTaskPriority priority,
            Long dueOffsetMinutes) {
        this.tenantId = tenantId;
        this.templateId = templateId;
        this.positionIndex = positionIndex;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueOffsetMinutes = dueOffsetMinutes;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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
}
