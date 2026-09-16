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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/projects/{projectId}/approvals/definitions")
public class ApprovalDefinitionController {

    private final ApprovalDefinitionService service;

    public ApprovalDefinitionController(ApprovalDefinitionService service) {
        this.service = service;
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks(#tenantId,#projectId,'project.task.read')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ApprovalDtos.DefinitionSummary>>> list(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval definitions fetched successfully",
                        service.list(tenantId, projectId, pageable)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canReadProjectTasks(#tenantId,#projectId,'project.task.read')")
    @GetMapping("/{definitionId}")
    public ResponseEntity<ApiResponse<ApprovalDtos.DefinitionResponse>> get(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval definition fetched successfully",
                        service.get(tenantId, projectId, definitionId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks(#tenantId,#projectId,'project.task.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<ApprovalDtos.DefinitionResponse>> create(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody ApprovalDtos.UpsertRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval definition created successfully",
                        service.create(tenantId, projectId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks(#tenantId,#projectId,'project.task.manage')")
    @PutMapping("/{definitionId}")
    public ResponseEntity<ApiResponse<ApprovalDtos.DefinitionResponse>> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId,
            @Valid @RequestBody ApprovalDtos.UpsertRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval definition updated successfully",
                        service.update(tenantId, projectId, definitionId, request)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks(#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{definitionId}/activate")
    public ResponseEntity<ApiResponse<ApprovalDtos.DefinitionResponse>> activate(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval definition activated successfully",
                        service.activate(tenantId, projectId, definitionId)));
    }

    @PreAuthorize(
            "@authorizationSecurity.canManageProjectTasks(#tenantId,#projectId,'project.task.manage')")
    @PostMapping("/{definitionId}/pause")
    public ResponseEntity<ApiResponse<ApprovalDtos.DefinitionResponse>> pause(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID definitionId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Approval definition paused successfully",
                        service.pause(tenantId, projectId, definitionId)));
    }
}
