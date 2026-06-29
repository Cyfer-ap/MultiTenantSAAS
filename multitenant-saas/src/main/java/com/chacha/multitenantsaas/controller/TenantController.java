package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantCreateRequest;
import com.chacha.multitenantsaas.dto.TenantResponse;
import com.chacha.multitenantsaas.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chacha.multitenantsaas.dto.TenantUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantStatusUpdateRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.common.SortingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;

@RestController
@Tag(
        name = "Tenants",
        description = "Tenant registration, listing, update, status, and soft-delete APIs"
)
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Operation(
            summary = "Create tenant",
            description = "Disabled. Use POST /api/onboarding/tenants for tenant onboarding."
    )
    @PreAuthorize("denyAll()")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody TenantCreateRequest request
    ) {
        TenantResponse tenant = tenantService.createTenant(request);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant created successfully", tenant)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "List tenants",
            description = "Disabled until SYSTEM_ADMIN role is implemented."
    )
    @PreAuthorize("denyAll()")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TenantResponse>>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(
                PaginationUtils.validatePage(page),
                PaginationUtils.validateSize(size),
                SortingUtils.getDirection(sortDir),
                SortingUtils.validateSortBy(
                        sortBy,
                        "createdAt",
                        "createdAt",
                        "name",
                        "slug",
                        "status"
                )
        );

        PageResponse<TenantResponse> tenants = tenantService.getAllTenants(
                status,
                search,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("Tenants fetched successfully", tenants)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get tenant by ID",
            description = "Returns details of a tenant if the authenticated user belongs to the same tenant."
    )
    @PreAuthorize("@tenantSecurity.isSameTenant(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(
            @PathVariable UUID id
    ) {
        TenantResponse tenant = tenantService.getTenantById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant fetched successfully", tenant)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get tenant by slug",
            description = "Returns tenant details by slug if the authenticated user belongs to that tenant."
    )
    @PreAuthorize("@tenantSecurity.isSameTenantBySlug(#slug)")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantBySlug(
            @PathVariable String slug
    ) {
        TenantResponse tenant = tenantService.getTenantBySlug(slug);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant fetched successfully", tenant)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update tenant",
            description = "Updates tenant name or slug. Only tenant admins can update their own tenant."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ){
        TenantResponse tenant = tenantService.updateTenant(id, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant updated successfully", tenant)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update tenant status",
            description = "Updates tenant status such as ACTIVE, INACTIVE, or SUSPENDED."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#id)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenantStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TenantStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TenantResponse tenant = tenantService.updateTenantStatus(id, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant status updated successfully", tenant)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Soft delete tenant",
            description = "Soft deletes a tenant by setting its status to INACTIVE."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> deactivateTenant(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TenantResponse tenant = tenantService.deactivateTenant(id, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant deactivated successfully", tenant)
        );
    }

}