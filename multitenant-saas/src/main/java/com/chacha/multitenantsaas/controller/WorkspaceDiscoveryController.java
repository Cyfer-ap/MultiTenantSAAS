package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryStartRequest;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryStartResponse;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyRequest;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyResponse;
import com.chacha.multitenantsaas.service.EmailWorkspaceDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/workspaces")
@Tag(
        name = "Workspace discovery",
        description = "Email verification and tenant workspace discovery before login")
public class WorkspaceDiscoveryController {

    private final EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService;

    public WorkspaceDiscoveryController(
            EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService) {
        this.emailWorkspaceDiscoveryService = emailWorkspaceDiscoveryService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start email verification for workspace discovery")
    public ResponseEntity<ApiResponse<WorkspaceDiscoveryStartResponse>> start(
            @Valid @RequestBody WorkspaceDiscoveryStartRequest request) {
        WorkspaceDiscoveryStartResponse response = emailWorkspaceDiscoveryService.start(request);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify email and return available workspaces")
    public ResponseEntity<ApiResponse<WorkspaceDiscoveryVerifyResponse>> verify(
            @Valid @RequestBody WorkspaceDiscoveryVerifyRequest request) {
        WorkspaceDiscoveryVerifyResponse response = emailWorkspaceDiscoveryService.verify(request);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }
}
