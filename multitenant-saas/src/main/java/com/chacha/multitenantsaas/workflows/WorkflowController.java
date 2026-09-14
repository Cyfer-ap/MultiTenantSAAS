package com.chacha.multitenantsaas.workflows;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WorkflowDtos.Response>>> list(
            @PathVariable UUID tenantId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflows fetched successfully",
                        workflowService.list(tenantId, pageable)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.read')")
    @GetMapping("/executions")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowDtos.ExecutionResponse>>> executions(
            @PathVariable UUID tenantId, @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow execution history fetched successfully",
                        workflowService.executionHistory(tenantId, pageable)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.read')")
    @GetMapping("/{workflowId}")
    public ResponseEntity<ApiResponse<WorkflowDtos.Response>> get(
            @PathVariable UUID tenantId, @PathVariable UUID workflowId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow fetched successfully",
                        workflowService.get(tenantId, workflowId)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.update')")
    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowDtos.Response>> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody WorkflowDtos.UpsertRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow created successfully",
                        workflowService.create(tenantId, request, jwt)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.update')")
    @PutMapping("/{workflowId}")
    public ResponseEntity<ApiResponse<WorkflowDtos.Response>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID workflowId,
            @Valid @RequestBody WorkflowDtos.UpsertRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow updated successfully",
                        workflowService.update(tenantId, workflowId, request)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.update')")
    @PostMapping("/{workflowId}/activate")
    public ResponseEntity<ApiResponse<WorkflowDtos.Response>> activate(
            @PathVariable UUID tenantId, @PathVariable UUID workflowId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow activated successfully",
                        workflowService.activate(tenantId, workflowId)));
    }

    @PreAuthorize("@authorizationSecurity.hasTenantPermission(#tenantId,'project.update')")
    @PostMapping("/{workflowId}/pause")
    public ResponseEntity<ApiResponse<WorkflowDtos.Response>> pause(
            @PathVariable UUID tenantId, @PathVariable UUID workflowId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow paused successfully",
                        workflowService.pause(tenantId, workflowId)));
    }
}
