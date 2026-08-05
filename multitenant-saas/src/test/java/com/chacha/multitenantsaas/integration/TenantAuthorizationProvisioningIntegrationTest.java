package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.AuthorizationProvisioningSummary;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationRoleRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import com.chacha.multitenantsaas.service.AuthorizationPermissionEvaluator;
import com.chacha.multitenantsaas.service.AuthorizationProvisioningService;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantAuthorizationProvisioningIntegrationTest {

    @Autowired
    private TenantOnboardingService
            tenantOnboardingService;

    @Autowired
    private AuthorizationProvisioningService
            authorizationProvisioningService;

    @Autowired
    private AuthorizationRoleService
            authorizationRoleService;

    @Autowired
    private AuthorizationUserRoleAssignmentService
            authorizationUserRoleAssignmentService;

    @Autowired
    private AuthorizationPermissionEvaluator
            authorizationPermissionEvaluator;

    @Autowired
    private AuthorizationRoleRepository
            authorizationRoleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void onboardingCreatesDefaultRolesAndAdminGrant() {
        String suffix = uniqueSuffix();

        TenantOnboardingResponse response =
                tenantOnboardingService
                        .onboardTenant(
                                new TenantOnboardingRequest(
                                        "Provisioned Tenant",
                                        "provisioned-" + suffix,
                                        "Initial Administrator",
                                        "initial.admin."
                                                + suffix
                                                + "@example.test",
                                        "StrongPassword@123"
                                )
                        );

        UUID tenantId =
                response.tenant().id();

        UUID administratorId =
                response.adminUser().id();

        assertEquals(
                3,
                authorizationRoleRepository
                        .countByTenant_Id(tenantId)
        );

        AuthorizationRoleResponse adminRole =
                authorizationRoleService
                        .getRoleByCode(
                                tenantId,
                                SystemRoleCodes.ADMIN
                        );

        AuthorizationRoleResponse managerRole =
                authorizationRoleService
                        .getRoleByCode(
                                tenantId,
                                SystemRoleCodes.MANAGER
                        );

        AuthorizationRoleResponse memberRole =
                authorizationRoleService
                        .getRoleByCode(
                                tenantId,
                                SystemRoleCodes.MEMBER
                        );

        assertEquals(
                20,
                adminRole.permissions().size()
        );

        assertTrue(
                adminRole.permissions()
                        .stream()
                        .anyMatch(
                                permission ->
                                        PlatformPermissionCodes
                                                .SUBSCRIPTION_READ
                                                .equals(permission.code())
                        )
        );

        assertFalse(
                managerRole.permissions().isEmpty()
        );

        assertFalse(
                memberRole.permissions().isEmpty()
        );

        List<AuthorizationUserRoleAssignmentResponse>
                assignments =
                authorizationUserRoleAssignmentService
                        .getEffectiveUserAssignments(
                                tenantId,
                                administratorId,
                                Instant.now()
                        );

        assertEquals(
                1,
                assignments.size()
        );

        assertEquals(
                SystemRoleCodes.ADMIN,
                assignments.getFirst().roleCode()
        );

        assertEquals(
                AuthorizationScopeType.TENANT,
                assignments.getFirst().scopeType()
        );

        assertTrue(
                authorizationPermissionEvaluator
                        .hasPermission(
                                tenantId,
                                administratorId,
                                PlatformPermissionCodes
                                        .AUTHORIZATION_MANAGE,
                                AuthorizationEvaluationContext
                                        .tenant()
                        )
        );
    }

    @Test
    void legacyRoleBackfillIsIdempotent() {
        Tenant tenant =
                createTenant(
                        "legacy-backfill"
                );

        AppUser administrator =
                createUser(
                        tenant,
                        "Legacy Administrator",
                        UserRole.TENANT_ADMIN,
                        UserStatus.ACTIVE
                );

        AppUser manager =
                createUser(
                        tenant,
                        "Legacy Manager",
                        UserRole.TENANT_MANAGER,
                        UserStatus.ACTIVE
                );

        AppUser member =
                createUser(
                        tenant,
                        "Legacy Member",
                        UserRole.TENANT_USER,
                        UserStatus.ACTIVE
                );

        AppUser inactiveMember =
                createUser(
                        tenant,
                        "Inactive Legacy Member",
                        UserRole.TENANT_USER,
                        UserStatus.INACTIVE
                );

        AuthorizationProvisioningSummary firstRun =
                authorizationProvisioningService
                        .provisionTenantFromLegacyRoles(
                                tenant.getId()
                        );

        assertEquals(
                3,
                firstRun.systemRolesAvailable()
        );

        assertEquals(
                4,
                firstRun.usersScanned()
        );

        assertEquals(
                3,
                firstRun.assignmentsCreated()
        );

        assertEquals(
                0,
                firstRun.assignmentsAlreadyPresent()
        );

        assertEquals(
                1,
                firstRun.inactiveUsersSkipped()
        );

        AuthorizationProvisioningSummary secondRun =
                authorizationProvisioningService
                        .provisionTenantFromLegacyRoles(
                                tenant.getId()
                        );

        assertEquals(
                0,
                secondRun.assignmentsCreated()
        );

        assertEquals(
                3,
                secondRun.assignmentsAlreadyPresent()
        );

        assertEquals(
                1,
                secondRun.inactiveUsersSkipped()
        );

        assertEquals(
                3,
                authorizationRoleRepository
                        .countByTenant_Id(
                                tenant.getId()
                        )
        );

        assertEquals(
                1,
                authorizationUserRoleAssignmentService
                        .getEffectiveUserAssignments(
                                tenant.getId(),
                                administrator.getId(),
                                Instant.now()
                        )
                        .size()
        );

        assertEquals(
                1,
                authorizationUserRoleAssignmentService
                        .getEffectiveUserAssignments(
                                tenant.getId(),
                                manager.getId(),
                                Instant.now()
                        )
                        .size()
        );

        assertEquals(
                1,
                authorizationUserRoleAssignmentService
                        .getEffectiveUserAssignments(
                                tenant.getId(),
                                member.getId(),
                                Instant.now()
                        )
                        .size()
        );

        assertTrue(
                authorizationUserRoleAssignmentService
                        .getUserAssignments(
                                tenant.getId(),
                                inactiveMember.getId()
                        )
                        .isEmpty()
        );

        assertTrue(
                hasTenantPermission(
                        tenant,
                        administrator,
                        PlatformPermissionCodes
                                .AUTHORIZATION_MANAGE
                )
        );

        assertTrue(
                hasTenantPermission(
                        tenant,
                        manager,
                        PlatformPermissionCodes
                                .PROJECT_CREATE
                )
        );

        assertFalse(
                hasTenantPermission(
                        tenant,
                        manager,
                        PlatformPermissionCodes
                                .AUTHORIZATION_MANAGE
                )
        );

        assertTrue(
                hasTenantPermission(
                        tenant,
                        member,
                        PlatformPermissionCodes
                                .PROJECT_READ
                )
        );

        assertFalse(
                hasTenantPermission(
                        tenant,
                        member,
                        PlatformPermissionCodes
                                .PROJECT_CREATE
                )
        );
    }

    private boolean hasTenantPermission(
            Tenant tenant,
            AppUser user,
            String permissionCode
    ) {
        return authorizationPermissionEvaluator
                .hasPermission(
                        tenant.getId(),
                        user.getId(),
                        permissionCode,
                        AuthorizationEvaluationContext.tenant()
                );
    }

    private Tenant createTenant(String prefix) {
        String suffix = uniqueSuffix();

        Tenant tenant = new Tenant(
                prefix + " Tenant",
                prefix + "-" + suffix
        );

        return tenantRepository
                .saveAndFlush(tenant);
    }

    private AppUser createUser(
            Tenant tenant,
            String fullName,
            UserRole role,
            UserStatus status
    ) {
        AppUser user = new AppUser(
                tenant,
                fullName,
                role.name()
                        .toLowerCase()
                        .replace('_', '.')
                        + "."
                        + uniqueSuffix()
                        + "@example.test",
                "test-password-hash",
                role
        );

        user.setStatus(status);

        return appUserRepository
                .saveAndFlush(user);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}