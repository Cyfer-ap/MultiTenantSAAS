package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuthorizationRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuthorizationRolePermissionRepository
        extends JpaRepository<
        AuthorizationRolePermission,
        UUID
        > {

    @Query("""
            SELECT mapping
            FROM AuthorizationRolePermission mapping
            JOIN FETCH mapping.permission permission
            LEFT JOIN FETCH permission.tenant permissionTenant
            WHERE mapping.tenant.id = :tenantId
              AND mapping.role.id = :roleId
            ORDER BY
                permission.category ASC,
                permission.code ASC
            """)
    List<AuthorizationRolePermission>
    findRolePermissions(
            @Param("tenantId")
            UUID tenantId,

            @Param("roleId")
            UUID roleId
    );

    long countByTenant_IdAndRole_Id(
            UUID tenantId,
            UUID roleId
    );

    void deleteByTenant_IdAndRole_Id(
            UUID tenantId,
            UUID roleId
    );
}