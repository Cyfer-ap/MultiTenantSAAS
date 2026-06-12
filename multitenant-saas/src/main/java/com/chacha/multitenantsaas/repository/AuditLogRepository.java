package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<AuditLog> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);
}