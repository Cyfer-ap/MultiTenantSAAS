package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.SystemAdminCreateRequest;
import com.chacha.multitenantsaas.dto.SystemAdminResponse;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.service.SystemAdminManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/admins")
public class SystemAdminManagementController {

    private final SystemAdminManagementService systemAdminManagementService;

    public SystemAdminManagementController(SystemAdminManagementService systemAdminManagementService) {
        this.systemAdminManagementService = systemAdminManagementService;
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PostMapping
    public ResponseEntity<ApiResponse<SystemAdminResponse>> createSystemAdmin(
            @Valid @RequestBody SystemAdminCreateRequest request
    ) {
        SystemAdminResponse response = systemAdminManagementService.createSystemAdmin(request);

        return ResponseEntity.ok(
                ApiResponse.success("System admin created successfully", response)
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

        PageResponse<SystemAdminResponse> response = systemAdminManagementService.getSystemAdmins(
                status,
                search,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("System admins fetched successfully", response)
        );
    }
}
