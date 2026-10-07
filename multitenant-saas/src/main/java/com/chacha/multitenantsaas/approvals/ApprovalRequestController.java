package com.chacha.multitenantsaas.approvals;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/approvals")
public class ApprovalRequestController {

    private final ApprovalRequestQueryService queryService;
    private final ApprovalRequestCommandService commandService;
    private final ApprovalExternalReviewerService externalReviewerService;

    public ApprovalRequestController(
            ApprovalRequestQueryService queryService,
            ApprovalRequestCommandService commandService,
            ApprovalExternalReviewerService externalReviewerService) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.externalReviewerService = externalReviewerService;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks(#tenantId,#projectId,'project.task.read')")
    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalDtos.RequestSummary>>> inbox(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PageableDefault(size = 25) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval inbox fetched successfully",
                        queryService.inbox(tenantId, projectId, pageable, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks(#tenantId,#projectId,'project.task.read')")
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalDtos.RequestSummary>>> history(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval request history fetched successfully",
                        queryService.history(tenantId, projectId, pageable)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks(#tenantId,#projectId,'project.task.read')")
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<ApprovalDtos.RequestResponse>> get(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval request fetched successfully",
                        queryService.get(tenantId, projectId, requestId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks(#tenantId,#projectId,'project.task.manage')")
    @GetMapping("/requests/{requestId}/external-reviewers")
    public ResponseEntity<ApiResponse<java.util.List<ApprovalDtos.ExternalReviewerResponse>>>
            externalReviewers(
                    @PathVariable UUID tenantId,
                    @PathVariable UUID projectId,
                    @PathVariable UUID requestId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "External approval reviewers fetched successfully",
                        externalReviewerService.list(tenantId, projectId, requestId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks(#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/requests/{requestId}/external-reviewers")
    public ResponseEntity<ApiResponse<ApprovalDtos.ExternalReviewerResponse>>
            assignExternalReviewer(
                    @PathVariable UUID tenantId,
                    @PathVariable UUID projectId,
                    @PathVariable UUID requestId,
                    @Valid @RequestBody ApprovalDtos.AssignExternalReviewerRequest request,
                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "External approval reviewer assigned successfully",
                        externalReviewerService.assign(
                                tenantId, projectId, requestId, request.grantId(), jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks(#tenantId,#projectId,'project.task.manage')")
    @DeleteMapping("/requests/{requestId}/external-reviewers/{grantId}")
    public ResponseEntity<ApiResponse<Void>> revokeExternalReviewer(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID requestId,
            @PathVariable UUID grantId) {
        externalReviewerService.revoke(tenantId, projectId, requestId, grantId);
        return ResponseEntity.ok(
                ApiResponse.success("External approval reviewer revoked successfully", null));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks(#tenantId,#projectId,'project.task.read')")
    @PostMapping("/requests/{requestId}/decision")
    public ResponseEntity<ApiResponse<ApprovalDtos.RequestResponse>> decide(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID requestId,
            @Valid @RequestBody ApprovalDtos.DecisionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval decision recorded successfully",
                        commandService.decide(tenantId, projectId, requestId, request, jwt)));
    }
}
