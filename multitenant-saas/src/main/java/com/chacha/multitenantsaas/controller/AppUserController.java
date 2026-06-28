package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AppUserCreateRequest;
import com.chacha.multitenantsaas.dto.AppUserResponse;
import com.chacha.multitenantsaas.dto.AppUserRoleUpdateRequest;
import com.chacha.multitenantsaas.dto.AppUserStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.AppUserUpdateRequest;
import com.chacha.multitenantsaas.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.common.SortingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

@RestController
@Tag(
        name = "Tenant Users",
        description = "User management APIs inside a tenant"
)
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/tenants/{tenantId}/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Operation(
            summary = "Create tenant user",
            description = "Creates a user inside a tenant. Only tenant admins are allowed."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @PostMapping
    public ResponseEntity<ApiResponse<AppUserResponse>> createUser(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppUserCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AppUserResponse user = appUserService.createUser(tenantId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("User created successfully", user)
        );
    }

    @Operation(
            summary = "List tenant users",
            description = "Returns paginated, searchable, filterable, and sortable users inside a tenant."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdminOrManager(#tenantId)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AppUserResponse>>> getUsersByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
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
                        "fullName",
                        "email",
                        "role",
                        "status"
                )
        );

        PageResponse<AppUserResponse> users = appUserService.getUsersByTenant(
                tenantId,
                role,
                status,
                search,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("Users fetched successfully", users)
        );
    }

    @Operation(
            summary = "Get tenant user by ID",
            description = "Returns one user from the tenant."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdminOrManager(#tenantId)")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AppUserResponse>> getUserById(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId
    ) {
        AppUserResponse user = appUserService.getUserByTenantAndId(tenantId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("User fetched successfully", user)
        );
    }

    @Operation(
            summary = "Update tenant user",
            description = "Updates a user inside a tenant. Only tenant admins are allowed."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<AppUserResponse>> updateUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @Valid @RequestBody AppUserUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AppUserResponse user = appUserService.updateUser(tenantId, userId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", user)
        );
    }

    @Operation(
            summary = "Update tenant user role",
            description = "Updates the role of a user inside a tenant. Only tenant admins are allowed."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<AppUserResponse>> updateUserRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @Valid @RequestBody AppUserRoleUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AppUserResponse user = appUserService.updateUserRole(tenantId, userId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("User role updated successfully", user)
        );
    }

    @Operation(
            summary = "Update tenant user status",
            description = "Updates the status of a user inside a tenant. Only tenant admins are allowed."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AppUserResponse>> updateUserStatus(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @Valid @RequestBody AppUserStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AppUserResponse user = appUserService.updateUserStatus(tenantId, userId, request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("User status updated successfully", user)
        );
    }

    @Operation(
            summary = "Deactivate tenant user",
            description = "Deactivates a user inside a tenant. Only tenant admins are allowed."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<AppUserResponse>> deactivateUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AppUserResponse user = appUserService.deactivateUser(tenantId, userId, jwt);

        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully", user)
        );
    }

}