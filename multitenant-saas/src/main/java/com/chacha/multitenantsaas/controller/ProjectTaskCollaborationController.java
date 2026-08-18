package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.TaskActivityResponse;
import com.chacha.multitenantsaas.dto.TaskAttachmentDownloadResponse;
import com.chacha.multitenantsaas.dto.TaskAttachmentInitiateRequest;
import com.chacha.multitenantsaas.dto.TaskAttachmentResponse;
import com.chacha.multitenantsaas.dto.TaskAttachmentUploadResponse;
import com.chacha.multitenantsaas.dto.TaskCommentCreateRequest;
import com.chacha.multitenantsaas.dto.TaskCommentResponse;
import com.chacha.multitenantsaas.dto.TaskCommentUpdateRequest;
import com.chacha.multitenantsaas.service.TaskActivityService;
import com.chacha.multitenantsaas.service.TaskAttachmentService;
import com.chacha.multitenantsaas.service.TaskCollaborationService;
import jakarta.validation.Valid;
import java.util.List;
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
    private final TaskAttachmentService taskAttachmentService;

    public ProjectTaskCollaborationController(
            TaskCollaborationService taskCollaborationService,
            TaskActivityService taskActivityService,
            TaskAttachmentService taskAttachmentService) {
        this.taskCollaborationService = taskCollaborationService;
        this.taskActivityService = taskActivityService;
        this.taskAttachmentService = taskAttachmentService;
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
        Pageable pageable = commentPageable(page, size, sortDir);
        PageResponse<TaskCommentResponse> response =
                taskCollaborationService.getComments(tenantId, projectId, taskId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Task comments fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/comments/pinned")
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> getPinnedComments(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId) {
        List<TaskCommentResponse> response =
                taskCollaborationService.getPinnedComments(tenantId, projectId, taskId);
        return ResponseEntity.ok(
                ApiResponse.success("Pinned task comments fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<PageResponse<TaskCommentResponse>>> getReplies(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Pageable pageable = commentPageable(page, size, sortDir);
        PageResponse<TaskCommentResponse> response =
                taskCollaborationService.getReplies(
                        tenantId, projectId, taskId, commentId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Task comment replies fetched successfully", response));
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
    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> createReply(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @Valid @RequestBody TaskCommentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TaskCommentResponse response =
                taskCollaborationService.createReply(
                        tenantId, projectId, taskId, commentId, request, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Task comment reply created successfully", response));
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
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/comments/{commentId}/pin")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> pinComment(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal Jwt jwt) {
        TaskCommentResponse response =
                taskCollaborationService.pinComment(tenantId, projectId, taskId, commentId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Task comment pinned successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks("
                    + "#tenantId,#projectId,'project.task.manage')")
    @DeleteMapping("/comments/{commentId}/pin")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> unpinComment(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal Jwt jwt) {
        TaskCommentResponse response =
                taskCollaborationService.unpinComment(tenantId, projectId, taskId, commentId, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Task comment unpinned successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/attachments")
    public ResponseEntity<ApiResponse<PageResponse<TaskAttachmentResponse>>> getAttachments(
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

        PageResponse<TaskAttachmentResponse> response =
                taskAttachmentService.getAttachments(tenantId, projectId, taskId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Task attachments fetched successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @PostMapping("/attachments/uploads")
    public ResponseEntity<ApiResponse<TaskAttachmentUploadResponse>> initiateAttachmentUpload(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskAttachmentInitiateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        TaskAttachmentUploadResponse response =
                taskAttachmentService.initiateUpload(tenantId, projectId, taskId, request, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Attachment upload initiated successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @PostMapping("/attachments/{attachmentId}/complete")
    public ResponseEntity<ApiResponse<TaskAttachmentResponse>> completeAttachmentUpload(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal Jwt jwt) {
        TaskAttachmentResponse response =
                taskAttachmentService.completeUpload(
                        tenantId, projectId, taskId, attachmentId, jwt);
        return ResponseEntity.ok(
                ApiResponse.success("Attachment upload completed successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<ApiResponse<TaskAttachmentDownloadResponse>> getAttachmentDownload(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId) {
        TaskAttachmentDownloadResponse response =
                taskAttachmentService.getDownload(tenantId, projectId, taskId, attachmentId);
        return ResponseEntity.ok(
                ApiResponse.success("Attachment download URL created successfully", response));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks("
                    + "#tenantId,#projectId,'project.task.read')")
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<TaskAttachmentResponse>> deleteAttachment(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal Jwt jwt) {
        TaskAttachmentResponse response =
                taskAttachmentService.deleteAttachment(
                        tenantId, projectId, taskId, attachmentId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted successfully", response));
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

    private Pageable commentPageable(int page, int size, String sortDir) {
        return PageRequest.of(
                PaginationUtils.validatePage(page),
                PaginationUtils.validateSize(size),
                SortingUtils.getDirection(sortDir),
                "createdAt");
    }
}
