package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AuditLogResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByTenant(
                tenantId,
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
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByTenantAndUser(
                tenantId,
                userId,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("User audit logs fetched successfully", auditLogs)
        );
    }
}