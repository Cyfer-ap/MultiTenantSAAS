package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.dto.OidcAuthorizationStartRequest;
import com.chacha.multitenantsaas.dto.OidcAuthorizationStartResponse;
import com.chacha.multitenantsaas.service.BrowserSessionCookieService;
import com.chacha.multitenantsaas.service.OidcAuthorizationService;
import com.chacha.multitenantsaas.service.OidcCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "OIDC Authentication", description = "Tenant-bound enterprise OIDC authentication")
public class OidcAuthenticationController {

    private final OidcAuthorizationService authorizationService;
    private final OidcCallbackService callbackService;
    private final BrowserSessionCookieService browserSessionCookieService;

    public OidcAuthenticationController(
            OidcAuthorizationService authorizationService,
            OidcCallbackService callbackService,
            BrowserSessionCookieService browserSessionCookieService) {
        this.authorizationService = authorizationService;
        this.callbackService = callbackService;
        this.browserSessionCookieService = browserSessionCookieService;
    }

    @PostMapping("/api/tenants/{tenantId}/auth/oidc/start")
    @Operation(summary = "Start tenant OIDC login")
    public ResponseEntity<ApiResponse<OidcAuthorizationStartResponse>> start(
            @PathVariable UUID tenantId,
            @RequestBody(required = false) OidcAuthorizationStartRequest request) {
        boolean keepSignedIn = request != null && Boolean.TRUE.equals(request.keepSignedIn());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiResponse.success(
                                "OIDC authorization started",
                                authorizationService.start(tenantId, keepSignedIn)));
    }

    @GetMapping("/api/auth/oidc/callback")
    @Operation(summary = "Complete tenant OIDC login")
    public ResponseEntity<ApiResponse<LoginResponse>> callback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false, name = "error") String providerError,
            HttpServletResponse servletResponse) {
        LoginResponse response = callbackService.authenticate(state, code, providerError);
        LoginResponse clientResponse =
                browserSessionCookieService.applyLoginSession(servletResponse, response);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("OIDC login successful", clientResponse));
    }
}
