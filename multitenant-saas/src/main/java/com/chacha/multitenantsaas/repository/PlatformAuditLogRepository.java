package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.PlatformAuditAction;
import com.chacha.multitenantsaas.entity.PlatformAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PlatformAuditLogRepository
        extends JpaRepository<PlatformAuditLog, UUID> {

    @Query("""
            SELECT auditLog
            FROM PlatformAuditLog auditLog
            LEFT JOIN auditLog.actorSystemAdmin actor
            LEFT JOIN auditLog.targetSystemAdmin target
            WHERE (:action IS NULL OR auditLog.action = :action)
              AND (:success IS NULL OR auditLog.success = :success)
              AND (
                    :search IS NULL
                    OR LOWER(actor.email) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(target.email) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(auditLog.message) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<PlatformAuditLog> findPlatformAuditLogs(
            @Param("action") PlatformAuditAction action,
            @Param("success") Boolean success,
            @Param("search") String search,
            Pageable pageable
    );
}