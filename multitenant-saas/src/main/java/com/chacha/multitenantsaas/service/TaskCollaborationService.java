package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.TaskCommentCreateRequest;
import com.chacha.multitenantsaas.dto.TaskCommentMentionResponse;
import com.chacha.multitenantsaas.dto.TaskCommentResponse;
import com.chacha.multitenantsaas.dto.TaskCommentUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.entity.TaskComment;
import com.chacha.multitenantsaas.entity.TaskCommentMention;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.repository.TaskCommentRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCollaborationService {

    private static final int MAX_PINNED_COMMENTS = 5;

    private final TaskCommentRepository taskCommentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentActorService currentActorService;
    private final TaskActivityService taskActivityService;
    private final AuditLogService auditLogService;
    private final TaskAttachmentService taskAttachmentService;
    private final TaskNotificationService taskNotificationService;

    public TaskCollaborationService(
            TaskCommentRepository taskCommentRepository,
            ProjectRepository projectRepository,
            ProjectTaskRepository projectTaskRepository,
            ProjectMemberRepository projectMemberRepository,
            AppUserRepository appUserRepository,
            CurrentActorService currentActorService,
            TaskActivityService taskActivityService,
            AuditLogService auditLogService,
            TaskAttachmentService taskAttachmentService,
            TaskNotificationService taskNotificationService) {
        this.taskCommentRepository = taskCommentRepository;
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.appUserRepository = appUserRepository;
        this.currentActorService = currentActorService;
        this.taskActivityService = taskActivityService;
        this.auditLogService = auditLogService;
        this.taskAttachmentService = taskAttachmentService;
        this.taskNotificationService = taskNotificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskCommentResponse> getComments(
            UUID tenantId, UUID projectId, UUID taskId, Pageable pageable) {
        getTaskOrThrow(tenantId, projectId, taskId);
        Page<TaskComment> comments =
                taskCommentRepository.findByTenant_IdAndProject_IdAndTask_IdAndParentCommentIsNull(
                        tenantId, projectId, taskId, pageable);
        return mapPage(comments);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskCommentResponse> getReplies(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId, Pageable pageable) {
        getTaskOrThrow(tenantId, projectId, taskId);
        TaskComment parent = getCommentOrThrow(tenantId, projectId, taskId, commentId);
        ensureTopLevelComment(parent);
        Page<TaskComment> replies =
                taskCommentRepository.findByTenant_IdAndProject_IdAndTask_IdAndParentComment_Id(
                        tenantId, projectId, taskId, commentId, pageable);
        return mapPage(replies);
    }

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getPinnedComments(UUID tenantId, UUID projectId, UUID taskId) {
        getTaskOrThrow(tenantId, projectId, taskId);
        return taskCommentRepository
                .findTop5ByTenant_IdAndProject_IdAndTask_IdAndParentCommentIsNullAndDeletedFalseAndPinnedAtIsNotNullOrderByPinnedAtDesc(
                        tenantId, projectId, taskId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public TaskCommentResponse createComment(
            UUID tenantId, UUID projectId, UUID taskId, TaskCommentCreateRequest request, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        Set<AppUser> mentionedUsers =
                resolveMentionedUsers(tenantId, projectId, request.mentionedUserIds());

        TaskComment comment =
                new TaskComment(
                        project.getTenant(), project, task, actor, normalizeBody(request.body()));
        comment.replaceMentions(mentionedUsers);
        TaskComment saved = taskCommentRepository.save(comment);

        taskActivityService.record(task, actor, TaskActivityType.COMMENT_ADDED, "Added a comment");
        recordAudit(
                project,
                task,
                actor,
                firstMentionOrActor(mentionedUsers, actor),
                AuditAction.TASK_COMMENT_CREATED,
                "Task comment created");
        taskNotificationService.notifyCommentCreated(task, saved, actor, mentionedUsers);
        return mapToResponse(saved);
    }

    @Transactional
    public TaskCommentResponse createReply(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID parentCommentId,
            TaskCommentCreateRequest request,
            Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TaskComment parent =
                getCommentForUpdateOrThrow(tenantId, projectId, taskId, parentCommentId);
        ensureTopLevelComment(parent);
        if (parent.isDeleted()) {
            throw new IllegalArgumentException("Deleted comments cannot receive new replies");
        }

        Set<AppUser> mentionedUsers =
                resolveMentionedUsers(tenantId, projectId, request.mentionedUserIds());
        TaskComment reply =
                new TaskComment(
                        project.getTenant(),
                        project,
                        task,
                        actor,
                        normalizeBody(request.body()),
                        parent);
        reply.replaceMentions(mentionedUsers);

        parent.incrementReplyCount();
        taskCommentRepository.save(parent);
        TaskComment saved = taskCommentRepository.save(reply);

        taskActivityService.record(
                task, actor, TaskActivityType.COMMENT_REPLIED, "Replied to a comment");
        recordAudit(
                project,
                task,
                actor,
                parent.getAuthorUser(),
                AuditAction.TASK_COMMENT_REPLIED,
                "Task comment reply created");
        taskNotificationService.notifyReplyCreated(task, saved, parent, actor, mentionedUsers);
        return mapToResponse(saved);
    }

    @Transactional
    public TaskCommentResponse updateComment(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID commentId,
            TaskCommentUpdateRequest request,
            Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TaskComment comment = getCommentOrThrow(tenantId, projectId, taskId, commentId);
        ensureCommentAuthor(comment, actor);
        if (comment.isDeleted()) {
            throw new IllegalArgumentException("Deleted comments cannot be edited");
        }

        Set<AppUser> mentionedUsers =
                resolveMentionedUsers(tenantId, projectId, request.mentionedUserIds());
        Set<AppUser> newlyMentionedUsers = findNewMentions(comment, mentionedUsers);
        comment.edit(normalizeBody(request.body()), mentionedUsers);
        TaskComment saved = taskCommentRepository.save(comment);

        taskActivityService.record(
                task, actor, TaskActivityType.COMMENT_EDITED, "Edited a comment");
        recordAudit(
                project,
                task,
                actor,
                firstMentionOrActor(mentionedUsers, actor),
                AuditAction.TASK_COMMENT_UPDATED,
                "Task comment updated");
        taskNotificationService.notifyMentionedUsers(task, saved, actor, newlyMentionedUsers);
        return mapToResponse(saved);
    }

    @Transactional
    public TaskCommentResponse deleteComment(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TaskComment comment = getCommentOrThrow(tenantId, projectId, taskId, commentId);
        ensureCommentAuthor(comment, actor);
        if (comment.isDeleted()) {
            return mapToResponse(comment);
        }

        comment.markDeleted();
        TaskComment saved = taskCommentRepository.save(comment);
        taskAttachmentService.deleteAttachmentsForComment(
                tenantId, projectId, taskId, commentId, project, task, actor);

        taskActivityService.record(
                task, actor, TaskActivityType.COMMENT_DELETED, "Deleted a comment");
        recordAudit(
                project,
                task,
                actor,
                actor,
                AuditAction.TASK_COMMENT_DELETED,
                "Task comment deleted");
        return mapToResponse(saved);
    }

    @Transactional
    public TaskCommentResponse pinComment(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TaskComment comment = getCommentForUpdateOrThrow(tenantId, projectId, taskId, commentId);
        ensureTopLevelComment(comment);
        if (comment.isDeleted()) {
            throw new IllegalArgumentException("Deleted comments cannot be pinned");
        }
        if (comment.isPinned()) {
            return mapToResponse(comment);
        }

        long pinnedCount =
                taskCommentRepository
                        .countByTenant_IdAndProject_IdAndTask_IdAndParentCommentIsNullAndDeletedFalseAndPinnedAtIsNotNull(
                                tenantId, projectId, taskId);
        if (pinnedCount >= MAX_PINNED_COMMENTS) {
            throw new IllegalArgumentException(
                    "A task can have at most " + MAX_PINNED_COMMENTS + " pinned comments");
        }

        comment.pin(actor);
        TaskComment saved = taskCommentRepository.save(comment);
        taskActivityService.record(
                task, actor, TaskActivityType.COMMENT_PINNED, "Pinned a comment");
        recordAudit(
                project,
                task,
                actor,
                comment.getAuthorUser(),
                AuditAction.TASK_COMMENT_PINNED,
                "Task comment pinned");
        return mapToResponse(saved);
    }

    @Transactional
    public TaskCommentResponse unpinComment(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId, Jwt jwt) {
        Project project = getProjectOrThrow(tenantId, projectId);
        ProjectTask task = getTaskOrThrow(tenantId, projectId, taskId);
        ensureCollaborationCanBeModified(project, task);

        AppUser actor = currentActorService.getRequiredActiveActor(tenantId, jwt);
        TaskComment comment = getCommentForUpdateOrThrow(tenantId, projectId, taskId, commentId);
        ensureTopLevelComment(comment);
        if (!comment.isPinned()) {
            return mapToResponse(comment);
        }

        comment.unpin();
        TaskComment saved = taskCommentRepository.save(comment);
        taskActivityService.record(
                task, actor, TaskActivityType.COMMENT_UNPINNED, "Unpinned a comment");
        recordAudit(
                project,
                task,
                actor,
                comment.getAuthorUser(),
                AuditAction.TASK_COMMENT_UNPINNED,
                "Task comment unpinned");
        return mapToResponse(saved);
    }

    private Set<AppUser> resolveMentionedUsers(
            UUID tenantId, UUID projectId, Set<UUID> mentionedUserIds) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
            return Set.of();
        }

        Set<AppUser> users = new LinkedHashSet<>();
        for (UUID userId : mentionedUserIds) {
            AppUser user =
                    appUserRepository
                            .findByTenantIdAndId(tenantId, userId)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Mentioned user not found with id: " + userId));
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new IllegalArgumentException("Only active project members can be mentioned");
            }
            if (!projectMemberRepository.existsByProject_Tenant_IdAndProject_IdAndUser_Id(
                    tenantId, projectId, userId)) {
                throw new IllegalArgumentException(
                        "Mentioned users must be members of the current project");
            }
            users.add(user);
        }
        return users;
    }

    private Set<AppUser> findNewMentions(TaskComment comment, Set<AppUser> mentionedUsers) {
        Set<UUID> existingUserIds = new LinkedHashSet<>();
        for (TaskCommentMention mention : comment.getMentions()) {
            existingUserIds.add(mention.getMentionedUser().getId());
        }

        Set<AppUser> newMentions = new LinkedHashSet<>();
        for (AppUser user : mentionedUsers) {
            if (!existingUserIds.contains(user.getId())) {
                newMentions.add(user);
            }
        }
        return newMentions;
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

    private TaskComment getCommentForUpdateOrThrow(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId) {
        return taskCommentRepository
                .findForUpdateByTenant_IdAndProject_IdAndTask_IdAndId(
                        tenantId, projectId, taskId, commentId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Task comment not found with id: " + commentId));
    }

    private void ensureTopLevelComment(TaskComment comment) {
        if (comment.getParentComment() != null) {
            throw new IllegalArgumentException("Replies cannot contain nested replies");
        }
    }

    private void ensureCollaborationCanBeModified(Project project, ProjectTask task) {
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived project collaboration is read-only");
        }
        if (task.getStatus() == ProjectTaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled task collaboration is read-only");
        }
    }

    private void ensureCommentAuthor(TaskComment comment, AppUser actor) {
        if (!comment.getAuthorUser().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the comment author can modify this comment");
        }
    }

    private String normalizeBody(String body) {
        String normalized = body.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Comment body cannot be blank");
        }
        return normalized;
    }

    private AppUser firstMentionOrActor(Set<AppUser> mentionedUsers, AppUser actor) {
        return mentionedUsers.stream().findFirst().orElse(actor);
    }

    private void recordAudit(
            Project project,
            ProjectTask task,
            AppUser actor,
            AppUser target,
            AuditAction action,
            String message) {
        auditLogService.recordSuccess(
                project.getTenant(), actor, target, action, message + " for task " + task.getId());
    }

    private PageResponse<TaskCommentResponse> mapPage(Page<TaskComment> comments) {
        return new PageResponse<>(
                comments.getContent().stream().map(this::mapToResponse).toList(),
                comments.getNumber(),
                comments.getSize(),
                comments.getTotalElements(),
                comments.getTotalPages(),
                comments.isFirst(),
                comments.isLast());
    }

    private TaskCommentResponse mapToResponse(TaskComment comment) {
        AppUser author = comment.getAuthorUser();
        List<TaskCommentMentionResponse> mentions =
                comment.getMentions().stream()
                        .map(TaskCommentMention::getMentionedUser)
                        .sorted(
                                Comparator.comparing(
                                        AppUser::getFullName, String.CASE_INSENSITIVE_ORDER))
                        .map(
                                user ->
                                        new TaskCommentMentionResponse(
                                                user.getId(), user.getFullName(), user.getEmail()))
                        .toList();
        TaskComment parent = comment.getParentComment();

        return new TaskCommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                parent == null ? null : parent.getId(),
                author.getId(),
                author.getFullName(),
                author.getEmail(),
                comment.isDeleted() ? null : comment.getBody(),
                comment.isDeleted(),
                comment.getReplyCount(),
                comment.isPinned(),
                comment.getPinnedAt(),
                comment.getPinnedByUserId(),
                comment.getEditedAt(),
                comment.getDeletedAt(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                mentions);
    }
}
