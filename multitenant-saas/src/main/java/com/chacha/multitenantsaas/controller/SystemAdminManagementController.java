package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.SystemAdminCreateRequest;
import com.chacha.multitenantsaas.dto.SystemAdminResponse;
import com.chacha.multitenantsaas.dto.SystemAdminStatusUpdateRequest;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.service.SystemAdminManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/system/admins")
public class SystemAdminManagementController {

    private final SystemAdminManagementService systemAdminManagementService;

    public SystemAdminManagementController(
            SystemAdminManagementService systemAdminManagementService
    ) {
        this.systemAdminManagementService = systemAdminManagementService;
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PostMapping
    public ResponseEntity<ApiResponse<SystemAdminResponse>> createSystemAdmin(
            @Valid @RequestBody SystemAdminCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        SystemAdminResponse response =
                systemAdminManagementService.createSystemAdmin(request, jwt);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "System admin created successfully",
                        response
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SystemAdminResponse>>> getSystemAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
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
                        "status"
                )
        );

        PageResponse<SystemAdminResponse> response =
                systemAdminManagementService.getSystemAdmins(
                        status,
                        search,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "System admins fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping("/{systemAdminId}")
    public ResponseEntity<ApiResponse<SystemAdminResponse>> getSystemAdminById(
            @PathVariable UUID systemAdminId
    ) {
        SystemAdminResponse response =
                systemAdminManagementService.getSystemAdminById(systemAdminId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "System admin fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PatchMapping("/{systemAdminId}/status")
    public ResponseEntity<ApiResponse<SystemAdminResponse>> updateSystemAdminStatus(
            @PathVariable UUID systemAdminId,
            @Valid @RequestBody SystemAdminStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        SystemAdminResponse response =
                systemAdminManagementService.updateSystemAdminStatus(
                        systemAdminId,
                        request,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "System admin status updated successfully",
                        response
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PatchMapping("/{systemAdminId}/unlock")
    public ResponseEntity<ApiResponse<SystemAdminResponse>> unlockSystemAdminLogin(
            @PathVariable UUID systemAdminId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        SystemAdminResponse response =
                systemAdminManagementService.unlockSystemAdminLogin(
                        systemAdminId,
                        jwt
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "System admin login unlocked successfully",
                        response
                )
        );
    }
}