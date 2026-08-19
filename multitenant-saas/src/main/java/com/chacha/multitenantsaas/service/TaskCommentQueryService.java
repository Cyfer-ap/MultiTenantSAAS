package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TaskCommentMentionResponse;
import com.chacha.multitenantsaas.dto.TaskCommentResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.TaskComment;
import com.chacha.multitenantsaas.entity.TaskCommentMention;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TaskCommentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskCommentQueryService {

    private final TaskCommentRepository taskCommentRepository;

    public TaskCommentQueryService(TaskCommentRepository taskCommentRepository) {
        this.taskCommentRepository = taskCommentRepository;
    }

    @Transactional(readOnly = true)
    public TaskCommentResponse getComment(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId) {
        TaskComment comment =
                taskCommentRepository
                        .findByTenant_IdAndProject_IdAndTask_IdAndId(
                                tenantId, projectId, taskId, commentId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Task comment not found with id: " + commentId));
        return mapToResponse(comment);
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
