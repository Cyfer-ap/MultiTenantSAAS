package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.AuthorizationPermissionResponse;
import com.chacha.multitenantsaas.dto.TenantPermissionCreateRequest;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.service.AuthorizationPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorizationPermissionCatalogIntegrationTest {

    @Autowired
    private AuthorizationPermissionService
            authorizationPermissionService;

    @Autowired
    private AuthorizationPermissionRepository
            authorizationPermissionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void platformPermissionCatalogIsSeeded() {
        List<AuthorizationPermission>
                platformPermissions =
                authorizationPermissionRepository
                        .findBySourceAndStatusOrderByCategoryAscCodeAsc(
                                AuthorizationPermissionSource.PLATFORM,
                                AuthorizationPermissionStatus.ACTIVE
                        );

        assertEquals(
                19,
                platformPermissions.size()
        );

        AuthorizationPermission assignmentPermission =
                authorizationPermissionRepository
                        .findBySourceAndCode(
                                AuthorizationPermissionSource.PLATFORM,
                                PlatformPermissionCodes
                                        .ORGANIZATION_ASSIGNMENT_MANAGE
                        )
                        .orElseThrow();

        assertNull(
                assignmentPermission.getTenant()
        );

        assertEquals(
                AuthorizationPermission
                        .PLATFORM_CATALOG_KEY,
                assignmentPermission.getCatalogKey()
        );

        assertEquals(
                AuthorizationPermissionStatus.ACTIVE,
                assignmentPermission.getStatus()
        );
    }

    @Test
    void createsAndNormalizesTenantCustomPermission() {
        Tenant tenant =
                createTenant("permission-create");

        AuthorizationPermissionResponse permission =
                authorizationPermissionService
                        .createTenantPermission(
                                tenant.getId(),
                                request(
                                        "  CUSTOM.Invoice.Approve  ",
                                        "  Approve invoices  ",
                                        "  Approve financial invoices.  ",
                                        "Finance Operations"
                                )
                        );

        assertNotNull(permission.id());

        assertEquals(
                tenant.getId(),
                permission.tenantId()
        );

        assertEquals(
                "custom.invoice.approve",
                permission.code()
        );

        assertEquals(
                "Approve invoices",
                permission.name()
        );

        assertEquals(
                "Approve financial invoices.",
                permission.description()
        );

        assertEquals(
                "FINANCE_OPERATIONS",
                permission.category()
        );

        assertEquals(
                AuthorizationPermissionSource.TENANT,
                permission.source()
        );

        assertEquals(
                AuthorizationPermissionStatus.ACTIVE,
                permission.status()
        );
    }

    @Test
    void duplicateCodeIsRejectedWithinTenantButAllowedAcrossTenants() {
        Tenant firstTenant =
                createTenant("permission-duplicate-first");

        Tenant secondTenant =
                createTenant("permission-duplicate-second");

        TenantPermissionCreateRequest request =
                request(
                        "custom.invoice.approve",
                        "Approve invoices",
                        null,
                        "FINANCE"
                );

        authorizationPermissionService
                .createTenantPermission(
                        firstTenant.getId(),
                        request
                );

        assertThrows(
                DuplicateResourceException.class,
                () ->
                        authorizationPermissionService
                                .createTenantPermission(
                                        firstTenant.getId(),
                                        request
                                )
        );

        AuthorizationPermissionResponse
                secondTenantPermission =
                authorizationPermissionService
                        .createTenantPermission(
                                secondTenant.getId(),
                                request
                        );

        assertEquals(
                secondTenant.getId(),
                secondTenantPermission.tenantId()
        );
    }

    @Test
    void availableCatalogContainsPlatformAndOnlyCurrentTenantPermissions() {
        Tenant firstTenant =
                createTenant("permission-catalog-first");

        Tenant secondTenant =
                createTenant("permission-catalog-second");

        AuthorizationPermissionResponse
                firstCustomPermission =
                authorizationPermissionService
                        .createTenantPermission(
                                firstTenant.getId(),
                                request(
                                        "custom.first.export",
                                        "Export first tenant data",
                                        null,
                                        "CUSTOM"
                                )
                        );

        AuthorizationPermissionResponse
                secondCustomPermission =
                authorizationPermissionService
                        .createTenantPermission(
                                secondTenant.getId(),
                                request(
                                        "custom.second.export",
                                        "Export second tenant data",
                                        null,
                                        "CUSTOM"
                                )
                        );

        List<AuthorizationPermissionResponse>
                firstTenantCatalog =
                authorizationPermissionService
                        .getAvailablePermissions(
                                firstTenant.getId()
                        );

        assertTrue(
                containsPermissionCode(
                        firstTenantCatalog,
                        PlatformPermissionCodes.PROJECT_READ
                )
        );

        assertTrue(
                containsPermissionId(
                        firstTenantCatalog,
                        firstCustomPermission.id()
                )
        );

        assertFalse(
                containsPermissionId(
                        firstTenantCatalog,
                        secondCustomPermission.id()
                )
        );
    }

    @Test
    void rejectsPlatformNamespaceAndInvalidCustomCodes() {
        Tenant tenant =
                createTenant("permission-invalid");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        authorizationPermissionService
                                .createTenantPermission(
                                        tenant.getId(),
                                        request(
                                                "project.read",
                                                "Fake project read",
                                                null,
                                                "PROJECT"
                                        )
                                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        authorizationPermissionService
                                .createTenantPermission(
                                        tenant.getId(),
                                        request(
                                                "custom.bad-code",
                                                "Bad custom code",
                                                null,
                                                "CUSTOM"
                                        )
                                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        authorizationPermissionService
                                .createTenantPermission(
                                        tenant.getId(),
                                        request(
                                                "custom.bad..value",
                                                "Empty code segment",
                                                null,
                                                "CUSTOM"
                                        )
                                )
        );
    }

    @Test
    void deactivatedPermissionIsHiddenAndTenantIsolationIsEnforced() {
        Tenant firstTenant =
                createTenant("permission-deactivate-first");

        Tenant secondTenant =
                createTenant("permission-deactivate-second");

        AuthorizationPermissionResponse permission =
                authorizationPermissionService
                        .createTenantPermission(
                                firstTenant.getId(),
                                request(
                                        "custom.report.export",
                                        "Export reports",
                                        null,
                                        "REPORTING"
                                )
                        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        authorizationPermissionService
                                .getPermission(
                                        secondTenant.getId(),
                                        permission.id()
                                )
        );

        AuthorizationPermissionResponse deactivated =
                authorizationPermissionService
                        .deactivateTenantPermission(
                                firstTenant.getId(),
                                permission.id()
                        );

        assertEquals(
                AuthorizationPermissionStatus.INACTIVE,
                deactivated.status()
        );

        List<AuthorizationPermissionResponse>
                availablePermissions =
                authorizationPermissionService
                        .getAvailablePermissions(
                                firstTenant.getId()
                        );

        assertFalse(
                containsPermissionId(
                        availablePermissions,
                        permission.id()
                )
        );

        assertTrue(
                containsPermissionCode(
                        availablePermissions,
                        PlatformPermissionCodes.AUDIT_READ
                )
        );
    }

    private boolean containsPermissionId(
            List<AuthorizationPermissionResponse> permissions,
            UUID permissionId
    ) {
        return permissions
                .stream()
                .anyMatch(
                        permission ->
                                permission.id()
                                        .equals(permissionId)
                );
    }

    private boolean containsPermissionCode(
            List<AuthorizationPermissionResponse> permissions,
            String permissionCode
    ) {
        return permissions
                .stream()
                .anyMatch(
                        permission ->
                                permission.code()
                                        .equals(permissionCode)
                );
    }

    private TenantPermissionCreateRequest request(
            String code,
            String name,
            String description,
            String category
    ) {
        return new TenantPermissionCreateRequest(
                code,
                name,
                description,
                category
        );
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