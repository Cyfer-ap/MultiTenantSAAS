package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.CurrentAuthorizationContextResponse;
import com.chacha.multitenantsaas.service.CurrentAuthorizationContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/tenants/{tenantId}/authorization"
)
public class CurrentAuthorizationContextController {

    private final CurrentAuthorizationContextService
            currentAuthorizationContextService;

    public CurrentAuthorizationContextController(
            CurrentAuthorizationContextService
                    currentAuthorizationContextService
    ) {
        this.currentAuthorizationContextService =
                currentAuthorizationContextService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".isCurrentTenant(#tenantId)"
    )
    @GetMapping("/me")
    public ResponseEntity<
            ApiResponse<
                    CurrentAuthorizationContextResponse
                    >
            >
    getCurrentAuthorizationContext(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        CurrentAuthorizationContextResponse response =
                currentAuthorizationContextService
                        .getCurrentAuthorizationContext(
                                tenantId,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Current authorization context "
                                + "fetched successfully",
                        response
                )
        );
    }
}