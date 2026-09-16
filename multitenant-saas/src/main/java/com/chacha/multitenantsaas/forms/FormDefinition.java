package com.chacha.multitenantsaas.forms;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "form_definitions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_form_definition_name",
                        columnNames = {"tenant_id", "project_id", "normalized_name"}),
        indexes =
                @Index(
                        name = "idx_form_definitions_project_status_name",
                        columnList = "tenant_id,project_id,status,name"))
public class FormDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormStatus status = FormStatus.DRAFT;

    @Column(name = "definition_version", nullable = false)
    private int definitionVersion = 1;

    @Column(name = "task_title_field_key", nullable = false, length = 64)
    private String taskTitleFieldKey;

    @Column(name = "task_description_field_key", length = 64)
    private String taskDescriptionFieldKey;

    @Column(name = "task_due_date_field_key", length = 64)
    private String taskDueDateFieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_priority", nullable = false, length = 30)
    private ProjectTaskPriority taskPriority;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("positionIndex ASC")
    private List<FormField> fields = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FormDefinition() {}

    public FormDefinition(
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            String name,
            String normalizedName,
            String description,
            String taskTitleFieldKey,
            String taskDescriptionFieldKey,
            String taskDueDateFieldKey,
            ProjectTaskPriority taskPriority,
            UUID workflowId) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.createdByUserId = createdByUserId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        this.taskTitleFieldKey = taskTitleFieldKey;
        this.taskDescriptionFieldKey = taskDescriptionFieldKey;
        this.taskDueDateFieldKey = taskDueDateFieldKey;
        this.taskPriority = taskPriority;
        this.workflowId = workflowId;
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
            String description,
            String taskTitleFieldKey,
            String taskDescriptionFieldKey,
            String taskDueDateFieldKey,
            ProjectTaskPriority taskPriority,
            UUID workflowId) {
        if (status == FormStatus.ACTIVE) {
            throw new IllegalStateException("Active forms must be paused before editing");
        }
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        this.taskTitleFieldKey = taskTitleFieldKey;
        this.taskDescriptionFieldKey = taskDescriptionFieldKey;
        this.taskDueDateFieldKey = taskDueDateFieldKey;
        this.taskPriority = taskPriority;
        this.workflowId = workflowId;
        this.definitionVersion++;
        this.updatedAt = Instant.now();
    }

    public void replaceFields(List<FormField> replacement) {
        if (status == FormStatus.ACTIVE) {
            throw new IllegalStateException("Active forms must be paused before editing");
        }
        fields.clear();
        fields.addAll(replacement);
    }

    public void activate() {
        status = FormStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void pause() {
        if (status != FormStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active forms can be paused");
        }
        status = FormStatus.PAUSED;
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

    public UUID getWorkflowId() {
        return workflowId;
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

    public FormStatus getStatus() {
        return status;
    }

    public int getDefinitionVersion() {
        return definitionVersion;
    }

    public String getTaskTitleFieldKey() {
        return taskTitleFieldKey;
    }

    public String getTaskDescriptionFieldKey() {
        return taskDescriptionFieldKey;
    }

    public String getTaskDueDateFieldKey() {
        return taskDueDateFieldKey;
    }

    public ProjectTaskPriority getTaskPriority() {
        return taskPriority;
    }

    public List<FormField> getFields() {
        return fields;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
