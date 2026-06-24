package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AuditLogResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getTenantAuditLogs(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Boolean success
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                getSortDirection(sortDir),
                "createdAt"
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

    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getUserAuditLogs(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Boolean success
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                getSortDirection(sortDir),
                "createdAt"
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


    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getUserAuditLogs(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Boolean success
    ) {
        Pageable pageable = PageRequest.of(page, size);

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