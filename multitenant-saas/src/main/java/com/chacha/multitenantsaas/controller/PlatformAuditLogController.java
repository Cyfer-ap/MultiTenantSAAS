package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.common.SortingUtils;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.PlatformAuditLogResponse;
import com.chacha.multitenantsaas.entity.PlatformAuditAction;
import com.chacha.multitenantsaas.service.PlatformAuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/audit-logs")
public class PlatformAuditLogController {

    private final PlatformAuditLogService platformAuditLogService;

    public PlatformAuditLogController(
            PlatformAuditLogService platformAuditLogService
    ) {
        this.platformAuditLogService = platformAuditLogService;
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<PlatformAuditLogResponse>>
            > getPlatformAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) PlatformAuditAction action,
            @RequestParam(required = false) Boolean success,
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
                        "action",
                        "success"
                )
        );

        PageResponse<PlatformAuditLogResponse> response =
                platformAuditLogService.getPlatformAuditLogs(
                        action,
                        success,
                        search,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Platform audit logs fetched successfully",
                        response
                )
        );
    }
}