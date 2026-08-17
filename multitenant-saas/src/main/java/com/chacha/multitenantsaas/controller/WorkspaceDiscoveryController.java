package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryStartRequest;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryStartResponse;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyRequest;
import com.chacha.multitenantsaas.dto.WorkspaceDiscoveryVerifyResponse;
import com.chacha.multitenantsaas.service.BrowserSessionCookieService;
import com.chacha.multitenantsaas.service.EmailWorkspaceDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final BrowserSessionCookieService browserSessionCookieService;

    public WorkspaceDiscoveryController(
            EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService,
            BrowserSessionCookieService browserSessionCookieService) {
        this.emailWorkspaceDiscoveryService = emailWorkspaceDiscoveryService;
        this.browserSessionCookieService = browserSessionCookieService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start email verification for workspace discovery")
    public ResponseEntity<ApiResponse<WorkspaceDiscoveryStartResponse>> start(
            @Valid @RequestBody WorkspaceDiscoveryStartRequest request,
            HttpServletRequest servletRequest) {
        WorkspaceDiscoveryStartRequest effectiveRequest =
                new WorkspaceDiscoveryStartRequest(
                        request.email(),
                        browserSessionCookieService.resolveTrustedBrowserToken(
                                servletRequest, request.trustedBrowserToken()));

        WorkspaceDiscoveryStartResponse response =
                emailWorkspaceDiscoveryService.start(effectiveRequest);

        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify email and return available workspaces")
    public ResponseEntity<ApiResponse<WorkspaceDiscoveryVerifyResponse>> verify(
            @Valid @RequestBody WorkspaceDiscoveryVerifyRequest request,
            HttpServletResponse servletResponse) {
        WorkspaceDiscoveryVerifyResponse response = emailWorkspaceDiscoveryService.verify(request);

        WorkspaceDiscoveryVerifyResponse clientResponse =
                browserSessionCookieService.applyTrustedBrowserSession(servletResponse, response);

        return ResponseEntity.ok(ApiResponse.success(clientResponse.message(), clientResponse));
    }
}
