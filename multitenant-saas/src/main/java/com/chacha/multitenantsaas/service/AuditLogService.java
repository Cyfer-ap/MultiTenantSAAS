package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuditLogResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.AuditLog;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.chacha.multitenantsaas.entity.AuditAction;
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

    public PageResponse<AuditLogResponse> getAuditLogsByTenant(
            UUID tenantId,
            AuditAction action,
            Boolean success,
            Pageable pageable
    ) {
        Page<AuditLog> auditLogs = auditLogRepository.findTenantAuditLogs(
                tenantId,
                action,
                success,
                pageable
        );

        return mapToPageResponse(auditLogs);
    }

    public PageResponse<AuditLogResponse> getAuditLogsByTenantAndUser(
            UUID tenantId,
            UUID userId,
            AuditAction action,
            Boolean success,
            Pageable pageable
    ) {
        Page<AuditLog> auditLogs = auditLogRepository.findUserAuditLogs(
                tenantId,
                userId,
                action,
                success,
                pageable
        );

        return mapToPageResponse(auditLogs);
    }


    private PageResponse<AuditLogResponse> mapToPageResponse(Page<AuditLog> auditLogs) {
        return new PageResponse<>(
                auditLogs.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList(),
                auditLogs.getNumber(),
                auditLogs.getSize(),
                auditLogs.getTotalElements(),
                auditLogs.getTotalPages(),
                auditLogs.isFirst(),
                auditLogs.isLast()
        );
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