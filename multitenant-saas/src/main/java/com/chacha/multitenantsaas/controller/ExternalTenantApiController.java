package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantApiContextResponse;
import com.chacha.multitenantsaas.security.TenantApiKeyPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/external/v1")
public class ExternalTenantApiController {

    @GetMapping("/context")
    public ResponseEntity<ApiResponse<TenantApiContextResponse>> getContext(
            @AuthenticationPrincipal TenantApiKeyPrincipal principal) {
        TenantApiContextResponse response =
                new TenantApiContextResponse(
                        principal.tenantId(),
                        principal.apiKeyId(),
                        principal.apiKeyName(),
                        principal.keyPrefix());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("API key context fetched successfully", response));
    }
}
