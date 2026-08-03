package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuthorizationRole;
import com.chacha.multitenantsaas.entity.AuthorizationRoleSource;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationRoleRepository
        extends JpaRepository<AuthorizationRole, UUID> {

    Optional<AuthorizationRole>
    findByTenant_IdAndId(
            UUID tenantId,
            UUID roleId
    );

    Optional<AuthorizationRole>
    findByTenant_IdAndCode(
            UUID tenantId,
            String code
    );

    boolean existsByTenant_IdAndCode(
            UUID tenantId,
            String code
    );

    long countByTenant_Id(UUID tenantId);

    long countByTenant_IdAndSource(
            UUID tenantId,
            AuthorizationRoleSource source
    );

    List<AuthorizationRole>
    findByTenant_IdOrderBySourceAscCodeAsc(
            UUID tenantId
    );

    List<AuthorizationRole>
    findByTenant_IdAndStatusOrderBySourceAscCodeAsc(
            UUID tenantId,
            AuthorizationRoleStatus status
    );
}