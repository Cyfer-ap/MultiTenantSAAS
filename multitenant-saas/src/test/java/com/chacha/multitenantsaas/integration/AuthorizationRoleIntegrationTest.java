package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.AuthorizationPermissionResponse;
import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.TenantPermissionCreateRequest;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.AuthorizationRoleSource;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.AuthorizationRoleRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import com.chacha.multitenantsaas.service.AuthorizationPermissionService;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorizationRoleIntegrationTest {

    @Autowired
    private AuthorizationRoleService
            authorizationRoleService;

    @Autowired
    private AuthorizationPermissionService
            authorizationPermissionService;

    @Autowired
    private AuthorizationRoleRepository
            authorizationRoleRepository;

    @Autowired
    private AuthorizationPermissionRepository
            authorizationPermissionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void initializesDefaultRolesIdempotently() {
        Tenant tenant =
                createTenant("role-defaults");

        List<AuthorizationRoleResponse> firstResult =
                authorizationRoleService
                        .initializeDefaultRoles(
                                tenant.getId()
                        );

        List<AuthorizationRoleResponse> secondResult =
                authorizationRoleService
                        .initializeDefaultRoles(
                                tenant.getId()
                        );

        assertEquals(3, firstResult.size());
        assertEquals(3, secondResult.size());

        assertEquals(
                3,
                authorizationRoleRepository
                        .countByTenant_Id(
                                tenant.getId()
                        )
        );

        AuthorizationRoleResponse adminRole =
                authorizationRoleService
                        .getRoleByCode(
                                tenant.getId(),
                                SystemRoleCodes.ADMIN
                        );

        AuthorizationRoleResponse managerRole =
                authorizationRoleService
                        .getRoleByCode(
                                tenant.getId(),
                                SystemRoleCodes.MANAGER
                        );

        AuthorizationRoleResponse memberRole =
                authorizationRoleService
                        .getRoleByCode(
                                tenant.getId(),
                                SystemRoleCodes.MEMBER
                        );

        assertEquals(
                AuthorizationRoleSource.SYSTEM,
                adminRole.source()
        );

        assertEquals(
                AuthorizationRoleStatus.ACTIVE,
                adminRole.status()
        );

        assertEquals(
                19,
                adminRole.permissions().size()
        );

        assertTrue(
                containsPermissionCode(
                        managerRole,
                        PlatformPermissionCodes.PROJECT_CREATE
                )
        );

        assertFalse(
                containsPermissionCode(
                        managerRole,
                        PlatformPermissionCodes
                                .AUTHORIZATION_MANAGE
                )
        );

        assertTrue(
                containsPermissionCode(
                        memberRole,
                        PlatformPermissionCodes.PROJECT_READ
                )
        );

        assertFalse(
                containsPermissionCode(
                        memberRole,
                        PlatformPermissionCodes.PROJECT_CREATE
                )
        );
    }

    @Test
    void createsCustomRoleWithPlatformAndTenantPermission() {
        Tenant tenant =
                createTenant("role-custom");

        AuthorizationPermission platformPermission =
                getPlatformPermission(
                        PlatformPermissionCodes.PROJECT_READ
                );

        AuthorizationPermissionResponse customPermission =
                authorizationPermissionService
                        .createTenantPermission(
                                tenant.getId(),
                                new TenantPermissionCreateRequest(
                                        "custom.invoice.approve",
                                        "Approve invoices",
                                        null,
                                        "FINANCE"
                                )
                        );

        AuthorizationRoleResponse role =
                authorizationRoleService
                        .createTenantRole(
                                tenant.getId(),
                                new AuthorizationRoleCreateRequest(
                                        " finance approver ",
                                        " Finance Approver ",
                                        " Approves invoices. ",
                                        Set.of(
                                                platformPermission.getId(),
                                                customPermission.id()
                                        )
                                )
                        );

        assertNotNull(role.id());

        assertEquals(
                "FINANCE_APPROVER",
                role.code()
        );

        assertEquals(
                "Finance Approver",
                role.name()
        );

        assertEquals(
                AuthorizationRoleSource.TENANT,
                role.source()
        );

        assertEquals(
                AuthorizationRoleStatus.ACTIVE,
                role.status()
        );

        assertEquals(
                2,
                role.permissions().size()
        );

        assertTrue(
                containsPermissionCode(
                        role,
                        PlatformPermissionCodes.PROJECT_READ
                )
        );

        assertTrue(
                containsPermissionCode(
                        role,
                        "custom.invoice.approve"
                )
        );
    }

    @Test
    void rejectsDuplicateAndReservedRoleCodes() {
        Tenant tenant =
                createTenant("role-duplicate");

        authorizationRoleService
                .createTenantRole(
                        tenant.getId(),
                        emptyRoleRequest(
                                "REPORT_VIEWER"
                        )
                );

        assertThrows(
                DuplicateResourceException.class,
                () ->
                        authorizationRoleService
                                .createTenantRole(
                                        tenant.getId(),
                                        emptyRoleRequest(
                                                "report-viewer"
                                        )
                                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        authorizationRoleService
                                .createTenantRole(
                                        tenant.getId(),
                                        emptyRoleRequest(
                                                SystemRoleCodes.ADMIN
                                        )
                                )
        );
    }

    @Test
    void rejectsPermissionOwnedByAnotherTenant() {
        Tenant firstTenant =
                createTenant("role-isolation-first");

        Tenant secondTenant =
                createTenant("role-isolation-second");

        AuthorizationPermissionResponse
                secondTenantPermission =
                authorizationPermissionService
                        .createTenantPermission(
                                secondTenant.getId(),
                                new TenantPermissionCreateRequest(
                                        "custom.second.export",
                                        "Export second tenant data",
                                        null,
                                        "CUSTOM"
                                )
                        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        authorizationRoleService
                                .createTenantRole(
                                        firstTenant.getId(),
                                        new AuthorizationRoleCreateRequest(
                                                "DATA_EXPORTER",
                                                "Data Exporter",
                                                null,
                                                Set.of(
                                                        secondTenantPermission
                                                                .id()
                                                )
                                        )
                                )
        );
    }

    @Test
    void replacesPermissionsAndDeactivatesCustomRole() {
        Tenant tenant =
                createTenant("role-update");

        AuthorizationPermission projectRead =
                getPlatformPermission(
                        PlatformPermissionCodes.PROJECT_READ
                );

        AuthorizationPermission auditRead =
                getPlatformPermission(
                        PlatformPermissionCodes.AUDIT_READ
                );

        AuthorizationRoleResponse role =
                authorizationRoleService
                        .createTenantRole(
                                tenant.getId(),
                                new AuthorizationRoleCreateRequest(
                                        "AUDITOR",
                                        "Auditor",
                                        null,
                                        Set.of(
                                                projectRead.getId()
                                        )
                                )
                        );

        AuthorizationRoleResponse updated =
                authorizationRoleService
                        .replaceTenantRolePermissions(
                                tenant.getId(),
                                role.id(),
                                Set.of(
                                        auditRead.getId()
                                )
                        );

        assertEquals(
                1,
                updated.permissions().size()
        );

        assertTrue(
                containsPermissionCode(
                        updated,
                        PlatformPermissionCodes.AUDIT_READ
                )
        );

        assertFalse(
                containsPermissionCode(
                        updated,
                        PlatformPermissionCodes.PROJECT_READ
                )
        );

        AuthorizationRoleResponse deactivated =
                authorizationRoleService
                        .deactivateTenantRole(
                                tenant.getId(),
                                role.id()
                        );

        assertEquals(
                AuthorizationRoleStatus.INACTIVE,
                deactivated.status()
        );

        List<AuthorizationRoleResponse> activeRoles =
                authorizationRoleService
                        .getActiveRoles(
                                tenant.getId()
                        );

        assertFalse(
                activeRoles
                        .stream()
                        .anyMatch(
                                activeRole ->
                                        activeRole.id()
                                                .equals(role.id())
                        )
        );
    }

    @Test
    void systemRolesCannotBeModifiedOrDeactivated() {
        Tenant tenant =
                createTenant("role-system-protection");

        authorizationRoleService
                .initializeDefaultRoles(
                        tenant.getId()
                );

        AuthorizationRoleResponse adminRole =
                authorizationRoleService
                        .getRoleByCode(
                                tenant.getId(),
                                SystemRoleCodes.ADMIN
                        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        authorizationRoleService
                                .replaceTenantRolePermissions(
                                        tenant.getId(),
                                        adminRole.id(),
                                        Set.of()
                                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        authorizationRoleService
                                .deactivateTenantRole(
                                        tenant.getId(),
                                        adminRole.id()
                                )
        );
    }

    private AuthorizationRoleCreateRequest
    emptyRoleRequest(String code) {
        return new AuthorizationRoleCreateRequest(
                code,
                code,
                null,
                Set.of()
        );
    }

    private AuthorizationPermission
    getPlatformPermission(String code) {
        return authorizationPermissionRepository
                .findBySourceAndCode(
                        AuthorizationPermissionSource.PLATFORM,
                        code
                )
                .orElseThrow();
    }

    private boolean containsPermissionCode(
            AuthorizationRoleResponse role,
            String permissionCode
    ) {
        return role.permissions()
                .stream()
                .map(
                        AuthorizationPermissionResponse::code
                )
                .anyMatch(permissionCode::equals);
    }

    private Tenant createTenant(String prefix) {
        String suffix =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        Tenant tenant = new Tenant(
                prefix + " Tenant",
                prefix + "-" + suffix
        );

        return tenantRepository
                .saveAndFlush(tenant);
    }
}