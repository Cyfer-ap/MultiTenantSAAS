package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuthorizationRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;

import java.util.Set;
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

    @Query("""
        SELECT mapping
        FROM AuthorizationRolePermission mapping
        JOIN FETCH mapping.role role
        JOIN FETCH mapping.permission permission
        LEFT JOIN FETCH permission.tenant permissionTenant
        WHERE mapping.tenant.id = :tenantId
          AND role.id IN :roleIds
          AND role.status = :activeRoleStatus
          AND permission.status = :activePermissionStatus
          AND (
                permission.source =
                    :platformPermissionSource
                OR permissionTenant.id = :tenantId
          )
        ORDER BY
            role.code ASC,
            permission.code ASC
        """)
    List<AuthorizationRolePermission>
    findActiveRolePermissions(
            @Param("tenantId")
            UUID tenantId,

            @Param("roleIds")
            Set<UUID> roleIds,

            @Param("activeRoleStatus")
            AuthorizationRoleStatus activeRoleStatus,

            @Param("activePermissionStatus")
            AuthorizationPermissionStatus
                    activePermissionStatus,

            @Param("platformPermissionSource")
            AuthorizationPermissionSource
                    platformPermissionSource
    );
}