package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationPermissionRepository
        extends JpaRepository<
        AuthorizationPermission,
        UUID
        > {

    Optional<AuthorizationPermission>
    findByCatalogKeyAndCode(
            String catalogKey,
            String code
    );

    Optional<AuthorizationPermission>
    findBySourceAndCode(
            AuthorizationPermissionSource source,
            String code
    );

    Optional<AuthorizationPermission>
    findByTenant_IdAndCode(
            UUID tenantId,
            String code
    );

    Optional<AuthorizationPermission>
    findByTenant_IdAndId(
            UUID tenantId,
            UUID permissionId
    );

    List<AuthorizationPermission>
    findBySourceAndStatusOrderByCategoryAscCodeAsc(
            AuthorizationPermissionSource source,
            AuthorizationPermissionStatus status
    );

    List<AuthorizationPermission>
    findByTenant_IdOrderByCategoryAscCodeAsc(
            UUID tenantId
    );

    @Query("""
            SELECT permission
            FROM AuthorizationPermission permission
            LEFT JOIN FETCH permission.tenant tenant
            WHERE permission.status = :status
              AND (
                    permission.source = :platformSource
                    OR tenant.id = :tenantId
              )
            ORDER BY
                permission.category ASC,
                permission.code ASC
            """)
    List<AuthorizationPermission>
    findAvailablePermissions(
            @Param("tenantId")
            UUID tenantId,

            @Param("platformSource")
            AuthorizationPermissionSource platformSource,

            @Param("status")
            AuthorizationPermissionStatus status
    );

    @Query("""
            SELECT permission
            FROM AuthorizationPermission permission
            LEFT JOIN FETCH permission.tenant tenant
            WHERE permission.id = :permissionId
              AND (
                    permission.source = :platformSource
                    OR tenant.id = :tenantId
              )
            """)
    Optional<AuthorizationPermission>
    findAccessiblePermissionById(
            @Param("tenantId")
            UUID tenantId,

            @Param("permissionId")
            UUID permissionId,

            @Param("platformSource")
            AuthorizationPermissionSource platformSource
    );

    @Query("""
            SELECT permission
            FROM AuthorizationPermission permission
            LEFT JOIN FETCH permission.tenant tenant
            WHERE permission.code = :code
              AND permission.status = :status
              AND (
                    permission.source = :platformSource
                    OR tenant.id = :tenantId
              )
            ORDER BY
                permission.source DESC
            """)
    List<AuthorizationPermission>
    findAccessiblePermissionsByCode(
            @Param("tenantId")
            UUID tenantId,

            @Param("code")
            String code,

            @Param("platformSource")
            AuthorizationPermissionSource platformSource,

            @Param("status")
            AuthorizationPermissionStatus status
    );
}