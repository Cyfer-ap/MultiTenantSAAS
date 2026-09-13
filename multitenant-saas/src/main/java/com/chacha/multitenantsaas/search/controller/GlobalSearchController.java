package com.chacha.multitenantsaas.search.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.search.model.GlobalSearchResponse;
import com.chacha.multitenantsaas.search.service.GlobalSearchService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/search")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    public GlobalSearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }

    @PreAuthorize("@authorizationSecurity.isCurrentTenant(#tenantId)")
    @GetMapping
    public ResponseEntity<ApiResponse<GlobalSearchResponse>> search(
            @PathVariable UUID tenantId,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "12") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        GlobalSearchResponse response = globalSearchService.search(tenantId, query, limit, jwt);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", response));
    }
}
