package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "task_attachments",
        indexes = {
            @Index(
                    name = "idx_task_attachment_task_created",
                    columnList = "tenant_id,project_id,task_id,created_at"),
            @Index(
                    name = "idx_task_attachment_comment",
                    columnList = "tenant_id,project_id,task_id,comment_id"),
            @Index(name = "idx_task_attachment_uploader", columnList = "tenant_id,uploader_user_id")
        })
public class TaskAttachment {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private ProjectTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private TaskComment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_user_id", nullable = false)
    private AppUser uploaderUser;

    @Column(name = "object_key", nullable = false, length = 700, unique = true)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "expected_size_bytes", nullable = false)
    private long expectedSizeBytes;

    @Column(name = "actual_size_bytes")
    private Long actualSizeBytes;

    @Column(name = "etag", length = 255)
    private String eTag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskAttachmentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public TaskAttachment() {}

    public TaskAttachment(
            UUID id,
            Tenant tenant,
            Project project,
            ProjectTask task,
            TaskComment comment,
            AppUser uploaderUser,
            String objectKey,
            String originalFilename,
            String contentType,
            long expectedSizeBytes) {
        this.id = id;
        this.tenant = tenant;
        this.project = project;
        this.task = task;
        this.comment = comment;
        this.uploaderUser = uploaderUser;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.expectedSizeBytes = expectedSizeBytes;
        this.status = TaskAttachmentStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public void markAvailable(long actualSizeBytes, String eTag) {
        this.actualSizeBytes = actualSizeBytes;
        this.eTag = eTag;
        this.status = TaskAttachmentStatus.AVAILABLE;
        this.completedAt = Instant.now();
    }

    public void markDeleted() {
        this.status = TaskAttachmentStatus.DELETED;
        this.deletedAt = Instant.now();
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

    public TaskComment getComment() {
        return comment;
    }

    public AppUser getUploaderUser() {
        return uploaderUser;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getExpectedSizeBytes() {
        return expectedSizeBytes;
    }

    public Long getActualSizeBytes() {
        return actualSizeBytes;
    }

    public String getETag() {
        return eTag;
    }

    public TaskAttachmentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
