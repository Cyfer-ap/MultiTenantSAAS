package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TaskCommentResponse;
import com.chacha.multitenantsaas.service.TaskCommentQueryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}/comments")
public class TaskCommentQueryController {

    private final TaskCommentQueryService taskCommentQueryService;

    public TaskCommentQueryController(TaskCommentQueryService taskCommentQueryService) {
        this.taskCommentQueryService = taskCommentQueryService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> getComment(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId) {
        TaskCommentResponse response =
                taskCommentQueryService.getComment(tenantId, projectId, taskId, commentId);
        return ResponseEntity.ok(
                ApiResponse.success("Task comment fetched successfully", response));
    }
}
