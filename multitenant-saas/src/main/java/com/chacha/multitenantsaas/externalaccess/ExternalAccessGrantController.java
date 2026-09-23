package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/tenants/{tenantId}/projects/{projectId}/external-access-grants")
public class ExternalAccessGrantController {

    private final ExternalAccessGrantService grantService;

    public ExternalAccessGrantController(ExternalAccessGrantService grantService) {
        this.grantService = grantService;
    }

    @PreAuthorize(
            "@authorizationSecurity.hasProjectPermission("
                    + "#tenantId,#projectId,'project.member.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<ExternalAccessDtos.GrantCreatedResponse>> create(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @Valid @RequestBody ExternalAccessDtos.CreateGrantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "External access grant created",
                                grantService.create(tenantId, projectId, request, jwt)));
    }

    @PreAuthorize(
            "@authorizationSecurity.hasProjectPermission("
                    + "#tenantId,#projectId,'project.member.manage')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExternalAccessDtos.GrantResponse>>> list(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable =
                PageRequest.of(
                        PaginationUtils.validatePage(page),
                        PaginationUtils.validateSize(size),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(
                ApiResponse.success(
                        "External access grants fetched",
                        grantService.list(tenantId, projectId, pageable)));
    }

    @PreAuthorize(
            "@authorizationSecurity.hasProjectPermission("
                    + "#tenantId,#projectId,'project.member.manage')")
    @DeleteMapping("/{grantId}")
    public ResponseEntity<ApiResponse<ExternalAccessDtos.GrantResponse>> revoke(
            @PathVariable UUID tenantId,
            @PathVariable UUID projectId,
            @PathVariable UUID grantId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "External access grant revoked",
                        grantService.revoke(tenantId, projectId, grantId, jwt)));
    }
}
