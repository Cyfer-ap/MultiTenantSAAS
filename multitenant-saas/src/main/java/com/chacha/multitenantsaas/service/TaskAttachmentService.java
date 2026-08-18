package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.TaskAttachmentDownloadResponse;
import com.chacha.multitenantsaas.dto.TaskAttachmentInitiateRequest;
import com.chacha.multitenantsaas.dto.TaskAttachmentResponse;
import com.chacha.multitenantsaas.dto.TaskAttachmentUploadResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.entity.TaskAttachment;
import com.chacha.multitenantsaas.entity.TaskAttachmentStatus;
import com.chacha.multitenantsaas.entity.TaskComment;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.repository.TaskAttachmentRepository;
import com.chacha.multitenantsaas.repository.TaskCommentRepository;
import com.chacha.multitenantsaas.storage.ObjectStorageService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(TaskAttachmentService.class);

    private final TaskAttachmentRepository taskAttachmentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final CurrentActorService currentActorService;
    private final TaskActivityService taskActivityService;
    private final AuditLogService auditLogService;
    private final ObjectProvider<ObjectStorageService> objectStorageServiceProvider;

    public TaskAttachmentService(
            TaskAttachmentRepository taskAttachmentRepository,
            ProjectRepository projectRepository,
            ProjectTaskRepository projectTaskRepository,
            TaskCommentRepository taskCommentRepository,
            CurrentActorService currentActorService,
            TaskActivityService taskActivityService,
            AuditLogService auditLogService,
            ObjectProvider<ObjectStorageService> objectStorageServiceProvider) {
        this.taskAttachmentRepository = taskAttachmentRepository;
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.currentActorService = currentActorService;
        this.taskActivityService = taskActivityService;
        this.auditLogService = auditLogService;
        this.objectStorageServiceProvider = objectStorageServiceProvider;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskAttachmentResponse> getAttachments(
            UUID tenantId, UUID projectId, UUID taskId, Pageable pageable) {
        getTaskOrThrow(tenantId, projectId, taskId);
        Page<TaskAttachment> attachments =
                taskAttachmentRepository.findVisibleTaskAttachments(
                        tenantId, projectId, taskId, TaskAttachmentStatus.AVAILABLE, pageable);

        return new PageResponse<>(
                attachments.getContent().stream().map(this::mapToResponse).toList(),
                attachments.getNumber(),
                attachments.getSize(),
                attachments.getTotalElements(),
                attachments.getTotalPages(),
                attachments.isFirst(),
                attachments.isLast());
    }

    @Transactional
    public TaskAttachmentUploadResponse initiateUpload(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            TaskAttachmentInitiateRequest request,
            Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TaskComment comment =
                request.commentId() == null
                        ? null
                        : getCommentOrThrow(tenantId, projectId, taskId, request.commentId());
        if (comment != null) {
            if (comment.isDeleted()) {
                throw new IllegalArgumentException("Files cannot be attached to a deleted comment");
            }
            if (!comment.getAuthorUser().getId().equals(actor.getId())) {
                throw new AccessDeniedException(
                        "Only the comment author can attach files to that comment");
            }
        }

        String filename = normalizeFilename(request.filename());
        String contentType = normalizeContentType(request.contentType());
        UUID attachmentId = UUID.randomUUID();
        String objectKey = buildObjectKey(tenantId, projectId, taskId, attachmentId);

        TaskAttachment attachment =
                new TaskAttachment(
                        attachmentId,
                        project.getTenant(),
                        project,
                        task,
                        comment,
                        actor,
                        objectKey,
                        filename,
                        contentType,
                        request.sizeBytes());
        TaskAttachment saved = taskAttachmentRepository.save(attachment);

        ObjectStorageService.PresignedUrl upload =
                requiredStorageService().presignUpload(objectKey, contentType);
        return new TaskAttachmentUploadResponse(
                mapToResponse(saved), upload.url(), upload.expiresAt(), upload.requiredHeaders());
    }

    @Transactional
    public TaskAttachmentResponse completeUpload(
            UUID tenantId, UUID projectId, UUID taskId, UUID attachmentId, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TaskAttachment attachment =
                getAttachmentForUpdateOrThrow(tenantId, projectId, taskId, attachmentId);
        ensureUploader(attachment, actor);

        if (attachment.getStatus() == TaskAttachmentStatus.AVAILABLE) {
            return mapToResponse(attachment);
        }
        if (attachment.getStatus() != TaskAttachmentStatus.PENDING) {
            throw new IllegalArgumentException("Attachment upload can no longer be completed");
        }

        ObjectStorageService.ObjectMetadata metadata =
                requiredStorageService().getObjectMetadata(attachment.getObjectKey());
        verifyUploadedObject(attachment, metadata);

        attachment.markAvailable(metadata.sizeBytes(), metadata.eTag());
        TaskAttachment saved = taskAttachmentRepository.save(attachment);

        taskActivityService.record(
                task,
                actor,
                TaskActivityType.ATTACHMENT_ADDED,
                "Attached " + attachment.getOriginalFilename());
        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                actor,
                AuditAction.TASK_ATTACHMENT_CREATED,
                "Task attachment created for task " + task.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TaskAttachmentDownloadResponse getDownload(
            UUID tenantId, UUID projectId, UUID taskId, UUID attachmentId) {
        getTaskOrThrow(tenantId, projectId, taskId);
        TaskAttachment attachment = getAttachmentOrThrow(tenantId, projectId, taskId, attachmentId);
        ensureVisible(attachment);

        ObjectStorageService.PresignedUrl download =
                requiredStorageService().presignDownload(attachment.getObjectKey());
        return new TaskAttachmentDownloadResponse(download.url(), download.expiresAt());
    }

    @Transactional
    public TaskAttachmentResponse deleteAttachment(
            UUID tenantId, UUID projectId, UUID taskId, UUID attachmentId, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);
        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TaskAttachment attachment =
                getAttachmentForUpdateOrThrow(tenantId, projectId, taskId, attachmentId);
        ensureUploader(attachment, actor);

        if (attachment.getStatus() == TaskAttachmentStatus.DELETED) {
            return mapToResponse(attachment);
        }

        boolean wasAvailable = attachment.getStatus() == TaskAttachmentStatus.AVAILABLE;
        requiredStorageService().deleteObject(attachment.getObjectKey());
        attachment.markDeleted();
        attachment.markStorageDeleted();
        TaskAttachment saved = taskAttachmentRepository.save(attachment);

        if (wasAvailable) {
            recordAttachmentDeleted(project, task, actor, attachment);
        }

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAttachmentsForComment(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID commentId,
            Project project,
            ProjectTask task,
            AppUser actor) {
        List<TaskAttachment> attachments =
                taskAttachmentRepository
                        .findByTenant_IdAndProject_IdAndTask_IdAndComment_IdAndStatusNot(
                                tenantId,
                                projectId,
                                taskId,
                                commentId,
                                TaskAttachmentStatus.DELETED);

        ObjectStorageService storageService = objectStorageServiceProvider.getIfAvailable();
        for (TaskAttachment attachment : attachments) {
            boolean wasAvailable = attachment.getStatus() == TaskAttachmentStatus.AVAILABLE;
            attachment.markDeleted();

            if (storageService != null) {
                try {
                    storageService.deleteObject(attachment.getObjectKey());
                    attachment.markStorageDeleted();
                } catch (RuntimeException exception) {
                    log.warn(
                            "Deferred storage cleanup for attachment {} after comment deletion",
                            attachment.getId(),
                            exception);
                }
            }

            taskAttachmentRepository.save(attachment);
            if (wasAvailable) {
                recordAttachmentDeleted(project, task, actor, attachment);
            }
        }
    }

    private Project getProjectOrThrow(UUID tenantId, UUID projectId) {
        return projectRepository
                .findByTenant_IdAndId(tenantId, projectId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Project not found with id: "
                                                + projectId
                                                + " for tenant: "
                                                + tenantId));
    }

    private ProjectTask getTaskOrThrow(UUID tenantId, UUID projectId, UUID taskId) {
        return projectTaskRepository
                .findByProject_Tenant_IdAndProject_IdAndId(tenantId, projectId, taskId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Task not found with id: "
                                                + taskId
                                                + " for project: "
                                                + projectId));
    }

    private TaskComment getCommentOrThrow(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId) {
        return taskCommentRepository
                .findByTenant_IdAndProject_IdAndTask_IdAndId(tenantId, projectId, taskId, commentId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Task comment not found with id: " + commentId));
    }

    private TaskAttachment getAttachmentOrThrow(
            UUID tenantId, UUID projectId, UUID taskId, UUID attachmentId) {
        return taskAttachmentRepository
                .findByTenant_IdAndProject_IdAndTask_IdAndId(
                        tenantId, projectId, taskId, attachmentId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Task attachment not found with id: " + attachmentId));
    }

    private TaskAttachment getAttachmentForUpdateOrThrow(
            UUID tenantId, UUID projectId, UUID taskId, UUID attachmentId) {
        return taskAttachmentRepository
                .findScopedForUpdate(tenantId, projectId, taskId, attachmentId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Task attachment not found with id: " + attachmentId));
    }

    private void ensureCollaborationCanBeModified(Project project, ProjectTask task) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived project collaboration is read-only");
        }
        if (task.getStatus() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled task collaboration is read-only");
        }
    }

    private void ensureUploader(TaskAttachment attachment, AppUser actor) {
        if (!attachment.getUploaderUser().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the attachment uploader can modify this file");
        }
    }

    private void ensureVisible(TaskAttachment attachment) {
        if (attachment.getStatus() != TaskAttachmentStatus.AVAILABLE) {
            throw new ResourceNotFoundException("Task attachment is not available");
        }
        if (attachment.getComment() != null && attachment.getComment().isDeleted()) {
            throw new ResourceNotFoundException("Task attachment is not available");
        }
    }

    private ObjectStorageService requiredStorageService() {
        ObjectStorageService storageService = objectStorageServiceProvider.getIfAvailable();
        if (storageService == null) {
            throw new IllegalStateException(
                    "Object storage is disabled. Configure STORAGE_PROVIDER=r2 to use attachments.");
        }
        return storageService;
    }

    private String normalizeFilename(String filename) {
        String normalized = filename.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("File name cannot be blank");
        }
        if (normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("File name cannot contain path separators");
        }
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("File name cannot contain control characters");
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Content type must be a valid MIME type");
        }
        if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
            throw new IllegalArgumentException("Content type cannot use wildcards");
        }
        return mediaType.toString();
    }

    private void verifyUploadedObject(
            TaskAttachment attachment, ObjectStorageService.ObjectMetadata metadata) {
        if (metadata.sizeBytes() != attachment.getExpectedSizeBytes()) {
            throw new IllegalArgumentException(
                    "Uploaded file size does not match the requested attachment size");
        }
        if (metadata.contentType() == null) {
            throw new IllegalArgumentException("Uploaded file is missing its content type");
        }

        MediaType expected = MediaType.parseMediaType(attachment.getContentType());
        MediaType actual = MediaType.parseMediaType(metadata.contentType());
        if (!expected.getType().equalsIgnoreCase(actual.getType())
                || !expected.getSubtype().equalsIgnoreCase(actual.getSubtype())) {
            throw new IllegalArgumentException(
                    "Uploaded file content type does not match the requested content type");
        }
    }

    private String buildObjectKey(UUID tenantId, UUID projectId, UUID taskId, UUID attachmentId) {
        return "tenants/"
                + tenantId
                + "/projects/"
                + projectId
                + "/tasks/"
                + taskId
                + "/attachments/"
                + attachmentId;
    }

    private void recordAttachmentDeleted(
            Project project, ProjectTask task, AppUser actor, TaskAttachment attachment) {
        taskActivityService.record(
                task,
                actor,
                TaskActivityType.ATTACHMENT_DELETED,
                "Deleted attachment " + attachment.getOriginalFilename());
        auditLogService.recordSuccess(
                project.getTenant(),
                actor,
                actor,
                AuditAction.TASK_ATTACHMENT_DELETED,
                "Task attachment deleted for task " + task.getId());
    }

    private TaskAttachmentResponse mapToResponse(TaskAttachment attachment) {
        AppUser uploader = attachment.getUploaderUser();
        UUID commentId = attachment.getComment() == null ? null : attachment.getComment().getId();
        long sizeBytes =
                attachment.getActualSizeBytes() == null
                        ? attachment.getExpectedSizeBytes()
                        : attachment.getActualSizeBytes();

        return new TaskAttachmentResponse(
                attachment.getId(),
                attachment.getTask().getId(),
                commentId,
                uploader.getId(),
                uploader.getFullName(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                sizeBytes,
                attachment.getStatus(),
                attachment.getCreatedAt(),
                attachment.getCompletedAt(),
                attachment.getDeletedAt());
    }
}
