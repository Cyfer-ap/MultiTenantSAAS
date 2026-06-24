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
import org.springframework.data.domain.Sort;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @PostMapping
    public ResponseEntity<ApiResponse<AppUserResponse>> createUser(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppUserCreateRequest request
    ) {
        AppUserResponse user = appUserService.createUser(tenantId, request);

        return ResponseEntity.ok(
                ApiResponse.success("User created successfully", user)
        );
    }

    @PreAuthorize("@tenantSecurity.isTenantAdminOrManager(#tenantId)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AppUserResponse>>> getUsersByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status
    ) {
        Pageable pageable = PageRequest.of(
                PaginationUtils.validatePage(page),
                PaginationUtils.validateSize(size),
                getSortDirection(sortDir),
                "createdAt"
        );

        PageResponse<AppUserResponse> users = appUserService.getUsersByTenant(
                tenantId,
                role,
                status,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("Users fetched successfully", users)
        );
    }

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

    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<AppUserResponse>> updateUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @Valid @RequestBody AppUserUpdateRequest request
    ) {
        AppUserResponse user = appUserService.updateUser(tenantId, userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", user)
        );
    }

    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<AppUserResponse>> updateUserRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @Valid @RequestBody AppUserRoleUpdateRequest request
    ) {
        AppUserResponse user = appUserService.updateUserRole(tenantId, userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("User role updated successfully", user)
        );
    }

    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AppUserResponse>> updateUserStatus(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @Valid @RequestBody AppUserStatusUpdateRequest request
    ) {
        AppUserResponse user = appUserService.updateUserStatus(tenantId, userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("User status updated successfully", user)
        );
    }

    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<AppUserResponse>> deactivateUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId
    ) {
        AppUserResponse user = appUserService.deactivateUser(tenantId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully", user)
        );
    }

    private Sort.Direction getSortDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.DESC;
        }

        throw new IllegalArgumentException("sortDir must be either 'asc' or 'desc'");
    }
}