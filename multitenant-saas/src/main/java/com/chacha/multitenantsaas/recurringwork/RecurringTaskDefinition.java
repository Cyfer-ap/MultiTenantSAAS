package com.chacha.multitenantsaas.recurringwork;

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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "recurring_task_definitions",
        indexes = {
            @Index(
                    name = "idx_recurring_task_definition_project",
                    columnList = "tenant_id,project_id,created_at"),
            @Index(
                    name = "idx_recurring_task_definition_due",
                    columnList = "status,next_occurrence_at")
        })
public class RecurringTaskDefinition {

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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectTaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceCadence cadence;

    @Column(name = "interval_count", nullable = false)
    private int intervalCount;

    @Column(name = "zone_id", nullable = false, length = 80)
    private String zoneId;

    @Column(name = "next_occurrence_at", nullable = false)
    private Instant nextOccurrenceAt;

    @Column(name = "due_offset_minutes")
    private Long dueOffsetMinutes;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    @Column(name = "generated_count", nullable = false)
    private int generatedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceStatus status;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecurringTaskDefinition() {}

    public RecurringTaskDefinition(
            UUID tenantId,
            UUID projectId,
            UUID createdByUserId,
            UUID assigneeUserId,
            String title,
            String description,
            ProjectTaskPriority priority,
            RecurrenceCadence cadence,
            int intervalCount,
            String zoneId,
            Instant nextOccurrenceAt,
            Long dueOffsetMinutes,
            Instant endAt,
            Integer maxOccurrences) {
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.createdByUserId = createdByUserId;
        this.assigneeUserId = assigneeUserId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.cadence = cadence;
        this.intervalCount = intervalCount;
        this.zoneId = zoneId;
        this.nextOccurrenceAt = nextOccurrenceAt;
        this.dueOffsetMinutes = dueOffsetMinutes;
        this.endAt = endAt;
        this.maxOccurrences = maxOccurrences;
        this.status = RecurrenceStatus.ACTIVE;
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ProjectTaskPriority getPriority() {
        return priority;
    }

    public RecurrenceCadence getCadence() {
        return cadence;
    }

    public int getIntervalCount() {
        return intervalCount;
    }

    public String getZoneId() {
        return zoneId;
    }

    public Instant getNextOccurrenceAt() {
        return nextOccurrenceAt;
    }

    public Long getDueOffsetMinutes() {
        return dueOffsetMinutes;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Integer getMaxOccurrences() {
        return maxOccurrences;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public RecurrenceStatus getStatus() {
        return status;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDefinition(
            UUID assigneeUserId,
            String title,
            String description,
            ProjectTaskPriority priority,
            RecurrenceCadence cadence,
            int intervalCount,
            String zoneId,
            Instant nextOccurrenceAt,
            Long dueOffsetMinutes,
            Instant endAt,
            Integer maxOccurrences) {
        this.assigneeUserId = assigneeUserId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.cadence = cadence;
        this.intervalCount = intervalCount;
        this.zoneId = zoneId;
        this.nextOccurrenceAt = nextOccurrenceAt;
        this.dueOffsetMinutes = dueOffsetMinutes;
        this.endAt = endAt;
        this.maxOccurrences = maxOccurrences;
        this.lastError = null;
        if (status == RecurrenceStatus.ENDED
                && (maxOccurrences == null || generatedCount < maxOccurrences)) {
            status = RecurrenceStatus.PAUSED;
        }
    }

    public void pause(String error) {
        status = RecurrenceStatus.PAUSED;
        lastError = error;
    }

    public void resumeAt(Instant occurrenceAt) {
        nextOccurrenceAt = occurrenceAt;
        status = RecurrenceStatus.ACTIVE;
        lastError = null;
    }

    public void markMaterialized(Instant nextAt) {
        generatedCount++;
        nextOccurrenceAt = nextAt;
        lastError = null;
    }

    public void end() {
        status = RecurrenceStatus.ENDED;
        lastError = null;
    }
}
