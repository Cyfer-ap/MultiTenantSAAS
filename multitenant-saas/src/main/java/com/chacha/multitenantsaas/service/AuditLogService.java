package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuditLogResponse;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.AuditLog;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
        record(
                tenant,
                user,
                user,
                action,
                success,
                message
        );
    }

    public void record(
            Tenant tenant,
            AppUser actorUser,
            AppUser targetUser,
            AuditAction action,
            boolean success,
            String message
    ) {
        AuditLog auditLog = new AuditLog(
                tenant,
                actorUser,
                targetUser,
                action,
                success,
                message
        );

        auditLogRepository.save(auditLog);
    }

    public void recordSystemAdmin(
            Tenant tenant,
            SystemAdmin actorSystemAdmin,
            AppUser targetUser,
            AuditAction action,
            boolean success,
            String message
    ) {
        AuditLog auditLog = new AuditLog(
                tenant,
                actorSystemAdmin,
                targetUser,
                action,
                success,
                message
        );

        auditLogRepository.save(auditLog);
    }

    public void recordSuccess(
            Tenant tenant,
            AppUser actorUser,
            AppUser targetUser,
            AuditAction action,
            String message
    ) {
        record(
                tenant,
                actorUser,
                targetUser,
                action,
                true,
                message
        );
    }

    public void recordFailure(
            Tenant tenant,
            AppUser actorUser,
            AppUser targetUser,
            AuditAction action,
            String message
    ) {
        record(
                tenant,
                actorUser,
                targetUser,
                action,
                false,
                message
        );
    }

    public void recordSelfSuccess(
            Tenant tenant,
            AppUser user,
            AuditAction action,
            String message
    ) {
        recordSuccess(
                tenant,
                user,
                user,
                action,
                message
        );
    }

    public void recordSelfFailure(
            Tenant tenant,
            AppUser user,
            AuditAction action,
            String message
    ) {
        recordFailure(
                tenant,
                user,
                user,
                action,
                message
        );
    }

    public void recordSystemFailure(
            Tenant tenant,
            AuditAction action,
            String message
    ) {
        recordFailure(
                tenant,
                null,
                null,
                action,
                message
        );
    }

    public void recordSystemAdminSuccess(
            Tenant tenant,
            SystemAdmin actorSystemAdmin,
            AppUser targetUser,
            AuditAction action,
            String message
    ) {
        recordSystemAdmin(
                tenant,
                actorSystemAdmin,
                targetUser,
                action,
                true,
                message
        );
    }

    public void recordSystemAdminFailure(
            Tenant tenant,
            SystemAdmin actorSystemAdmin,
            AppUser targetUser,
            AuditAction action,
            String message
    ) {
        recordSystemAdmin(
                tenant,
                actorSystemAdmin,
                targetUser,
                action,
                false,
                message
        );
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
        AppUser actorUser = auditLog.getActorUser();
        SystemAdmin actorSystemAdmin = auditLog.getActorSystemAdmin();
        AppUser targetUser = auditLog.getTargetUser();

        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getTenant().getId(),
                auditLog.getActorType(),
                actorUser != null ? actorUser.getId() : null,
                actorUser != null ? actorUser.getEmail() : null,
                actorSystemAdmin != null ? actorSystemAdmin.getId() : null,
                actorSystemAdmin != null ? actorSystemAdmin.getEmail() : null,
                targetUser != null ? targetUser.getId() : null,
                targetUser != null ? targetUser.getEmail() : null,
                auditLog.getAction(),
                auditLog.isSuccess(),
                auditLog.getMessage(),
                auditLog.getCreatedAt()
        );
    }
}