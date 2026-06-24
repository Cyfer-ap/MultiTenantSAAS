package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT auditLog
            FROM AuditLog auditLog
            WHERE auditLog.tenant.id = :tenantId
              AND (:action IS NULL OR auditLog.action = :action)
              AND (:success IS NULL OR auditLog.success = :success)
            """)
    Page<AuditLog> findTenantAuditLogs(
            @Param("tenantId") UUID tenantId,
            @Param("action") AuditAction action,
            @Param("success") Boolean success,
            Pageable pageable
    );

    @Query("""
            SELECT auditLog
            FROM AuditLog auditLog
            WHERE auditLog.tenant.id = :tenantId
              AND auditLog.user.id = :userId
              AND (:action IS NULL OR auditLog.action = :action)
              AND (:success IS NULL OR auditLog.success = :success)
            """)
    Page<AuditLog> findUserAuditLogs(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            @Param("action") AuditAction action,
            @Param("success") Boolean success,
            Pageable pageable
    );
}