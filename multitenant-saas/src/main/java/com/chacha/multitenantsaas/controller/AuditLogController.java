package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.AuditLogResponse;
import com.chacha.multitenantsaas.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getTenantAuditLogs(
            @PathVariable UUID tenantId
    ) {
        List<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByTenant(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success("Audit logs fetched successfully", auditLogs)
        );
    }

    @PreAuthorize("@tenantSecurity.isTenantAdmin(#tenantId)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getUserAuditLogs(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId
    ) {
        List<AuditLogResponse> auditLogs = auditLogService.getAuditLogsByTenantAndUser(
                tenantId,
                userId
        );

        return ResponseEntity.ok(
                ApiResponse.success("User audit logs fetched successfully", auditLogs)
        );
    }
}