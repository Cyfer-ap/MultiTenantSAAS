package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.PlatformAuditLogResponse;
import com.chacha.multitenantsaas.entity.PlatformAuditAction;
import com.chacha.multitenantsaas.entity.PlatformAuditLog;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.repository.PlatformAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformAuditLogService {

    private final PlatformAuditLogRepository platformAuditLogRepository;

    public PlatformAuditLogService(PlatformAuditLogRepository platformAuditLogRepository) {
        this.platformAuditLogRepository = platformAuditLogRepository;
    }

    public void record(
            SystemAdmin actorSystemAdmin,
            SystemAdmin targetSystemAdmin,
            PlatformAuditAction action,
            boolean success,
            String message) {
        PlatformAuditLog auditLog =
                new PlatformAuditLog(actorSystemAdmin, targetSystemAdmin, action, success, message);

        platformAuditLogRepository.save(auditLog);
    }

    public void recordSuccess(
            SystemAdmin actorSystemAdmin,
            SystemAdmin targetSystemAdmin,
            PlatformAuditAction action,
            String message) {
        record(actorSystemAdmin, targetSystemAdmin, action, true, message);
    }

    public void recordFailure(
            SystemAdmin actorSystemAdmin,
            SystemAdmin targetSystemAdmin,
            PlatformAuditAction action,
            String message) {
        record(actorSystemAdmin, targetSystemAdmin, action, false, message);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformAuditLogResponse> getPlatformAuditLogs(
            PlatformAuditAction action, Boolean success, String search, Pageable pageable) {
        Page<PlatformAuditLog> auditLogs =
                platformAuditLogRepository.findPlatformAuditLogs(
                        action, success, normalizeSearch(search), pageable);

        return new PageResponse<>(
                auditLogs.getContent().stream().map(this::mapToResponse).toList(),
                auditLogs.getNumber(),
                auditLogs.getSize(),
                auditLogs.getTotalElements(),
                auditLogs.getTotalPages(),
                auditLogs.isFirst(),
                auditLogs.isLast());
    }

    private PlatformAuditLogResponse mapToResponse(PlatformAuditLog auditLog) {
        SystemAdmin actor = auditLog.getActorSystemAdmin();
        SystemAdmin target = auditLog.getTargetSystemAdmin();

        return new PlatformAuditLogResponse(
                auditLog.getId(),
                actor != null ? actor.getId() : null,
                actor != null ? actor.getEmail() : null,
                target != null ? target.getId() : null,
                target != null ? target.getEmail() : null,
                auditLog.getAction(),
                auditLog.isSuccess(),
                auditLog.getMessage(),
                auditLog.getCreatedAt());
    }

    private String normalizeSearch(String search) {
        if (search == null || search.trim().isBlank()) {
            return null;
        }

        return search.trim();
    }
}
