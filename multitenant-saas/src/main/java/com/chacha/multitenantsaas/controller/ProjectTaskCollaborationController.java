package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.TaskActivityResponse;
import com.chacha.multitenantsaas.dto.TaskCommentCreateRequest;
import com.chacha.multitenantsaas.dto.TaskCommentResponse;
import com.chacha.multitenantsaas.dto.TaskCommentUpdateRequest;
import com.chacha.multitenantsaas.service.TaskActivityService;
import com.chacha.multitenantsaas.service.TaskCollaborationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}")
public class ProjectTaskCollaborationController {

    private final TaskCollaborationService taskCollaborationService;
    private final TaskActivityService taskActivityService;

    public ProjectTaskCollaborationController(
            TaskCollaborationService taskCollaborationService,
            TaskActivityService taskActivityService) {
        this.taskCollaborationService = taskCollaborationService;
        this.taskActivityService = taskActivityService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<PageResponse<TaskCommentResponse>>> getComments(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable =
                PageRequest.of(
                        PaginationUtils.validatePage(page),
                        PaginationUtils.validateSize(size),
                        SortingUtils.getDirection(sortDir),
                        "createdAt");

        PageResponse<TaskCommentResponse> response =
                taskCollaborationService.getComments(tenantId, projectId, taskId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Task comments fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @PostMapping("/comments")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> createComment(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskCommentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TaskCommentResponse response =
                taskCollaborationService.createComment(tenantId, projectId, taskId, request, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Task comment created successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> updateComment(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @Valid @RequestBody TaskCommentUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TaskCommentResponse response =
                taskCollaborationService.updateComment(
                        tenantId, projectId, taskId, commentId, request, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Task comment updated successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> deleteComment(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal Jwt jwt) {
        TaskCommentResponse response =
                taskCollaborationService.deleteComment(tenantId, projectId, taskId, commentId, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Task comment deleted successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<PageResponse<TaskActivityResponse>>> getActivity(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable =
                PageRequest.of(
                        PaginationUtils.validatePage(page),
                        PaginationUtils.validateSize(size),
                        SortingUtils.getDirection(sortDir),
                        "createdAt");

        PageResponse<TaskActivityResponse> response =
                taskActivityService.getActivities(tenantId, projectId, taskId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Task activity fetched successfully", response));
    }
}
