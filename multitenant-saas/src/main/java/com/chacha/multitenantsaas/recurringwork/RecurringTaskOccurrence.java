package com.chacha.multitenantsaas.recurringwork;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "recurring_task_occurrences",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_recurring_task_occurrence",
                        columnNames = {"definition_id", "scheduled_for"}),
        indexes =
                @Index(
                        name = "idx_recurring_task_occurrence_definition",
                        columnList = "tenant_id,project_id,definition_id,scheduled_for"))
public class RecurringTaskOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "definition_id", nullable = false)
    private UUID definitionId;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecurringTaskOccurrence() {}

    public RecurringTaskOccurrence(
            UUID tenantId,
            UUID projectId,
            UUID definitionId,
            Instant scheduledFor,
            UUID taskId) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.definitionId = definitionId;
        this.scheduledFor = scheduledFor;
        this.taskId = taskId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDefinitionId() {
        return definitionId;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
