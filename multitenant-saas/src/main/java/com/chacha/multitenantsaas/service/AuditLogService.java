package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuditLogResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.AuditLog;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(
            Tenant tenant,
            AppUser user,
            AuditAction action,
            boolean success,
            String message
    ) {
        AuditLog auditLog = new AuditLog(
                tenant,
                user,
                action,
                success,
                message
        );

        auditLogRepository.save(auditLog);
    }

    public List<AuditLogResponse> getAuditLogsByTenant(UUID tenantId) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsByTenantAndUser(UUID tenantId, UUID userId) {
        return auditLogRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        AppUser user = auditLog.getUser();

        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getTenant().getId(),
                user != null ? user.getId() : null,
                user != null ? user.getEmail() : null,
                auditLog.getAction(),
                auditLog.isSuccess(),
                auditLog.getMessage(),
                auditLog.getCreatedAt()
        );
    }
}