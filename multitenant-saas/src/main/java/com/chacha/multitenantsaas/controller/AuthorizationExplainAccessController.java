package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AuthorizationExplainAccessRequest;
import com.chacha.multitenantsaas.dto.AuthorizationExplainAccessResponse;
import com.chacha.multitenantsaas.service.AuthorizationAccessExplanationCommandService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/authorization")
public class AuthorizationExplainAccessController {

    private final AuthorizationAccessExplanationCommandService
            authorizationAccessExplanationCommandService;

    public AuthorizationExplainAccessController(
            AuthorizationAccessExplanationCommandService
                    authorizationAccessExplanationCommandService) {
        this.authorizationAccessExplanationCommandService =
                authorizationAccessExplanationCommandService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermission("
                    + "#tenantId,"
                    + "'authorization.manage'"
                    + ")")
    @PostMapping("/explain-access")
    public ResponseEntity<ApiResponse<AuthorizationExplainAccessResponse>> explainAccess(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AuthorizationExplainAccessRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AuthorizationExplainAccessResponse response =
                authorizationAccessExplanationCommandService.explainAccess(tenantId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Authorization access explained successfully", response));
    }
}
