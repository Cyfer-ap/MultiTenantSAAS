package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.common.PaginationUtils;
import com.chacha.multitenantsaas.dto.AuditLogResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.chacha.multitenantsaas.common.SortingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/audit-logs")
@Tag(
        name = "Audit Logs",
        description = "Tenant audit log APIs for admin activity tracking"
)
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(
            summary = "List tenant audit logs",
            description = "Returns paginated, filterable, and sortable audit logs for a tenant."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getTenantAuditLogs(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Boolean success
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

        PageResponse<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByTenant(
                tenantId,
                action,
                success,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("Audit logs fetched successfully", auditLogs)
        );
    }

    @Operation(
            summary = "Get user audit logs",
            description = "Returns paginated, filterable, and sortable audit logs for a specific user within a tenant."
    )
    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getUserAuditLogs(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Boolean success
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

        PageResponse<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByTenantAndUser(
                tenantId,
                userId,
                action,
                success,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("User audit logs fetched successfully", auditLogs)
        );
    }


}