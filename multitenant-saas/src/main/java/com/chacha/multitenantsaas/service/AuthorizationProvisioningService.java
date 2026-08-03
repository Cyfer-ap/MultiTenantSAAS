package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationProvisioningSummary;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignmentStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationUserRoleAssignmentRepository;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthorizationProvisioningService {

    private static final String TENANT_SCOPE_KEY =
            "TENANT";

    private final AppUserRepository appUserRepository;

    private final AuthorizationRoleService
            authorizationRoleService;

    private final AuthorizationUserRoleAssignmentService
            authorizationUserRoleAssignmentService;

    private final AuthorizationUserRoleAssignmentRepository
            authorizationUserRoleAssignmentRepository;

    public AuthorizationProvisioningService(
            AppUserRepository appUserRepository,
            AuthorizationRoleService
                    authorizationRoleService,
            AuthorizationUserRoleAssignmentService
                    authorizationUserRoleAssignmentService,
            AuthorizationUserRoleAssignmentRepository
                    authorizationUserRoleAssignmentRepository
    ) {
        this.appUserRepository =
                appUserRepository;

        this.authorizationRoleService =
                authorizationRoleService;

        this.authorizationUserRoleAssignmentService =
                authorizationUserRoleAssignmentService;

        this.authorizationUserRoleAssignmentRepository =
                authorizationUserRoleAssignmentRepository;
    }

    /**
     * Used during new tenant onboarding.
     *
     * Initializes all system roles and gives the initial legacy
     * TENANT_ADMIN user a tenant-wide V2 ADMIN assignment.
     */
    @Transactional
    public AuthorizationProvisioningSummary
    provisionInitialTenantAdministrator(
            UUID tenantId,
            UUID administratorUserId
    ) {
        AppUser administrator =
                getRequiredUser(
                        tenantId,
                        administratorUserId
                );

        if (administrator.getStatus()
                != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Initial tenant administrator "
                            + "must be active."
            );
        }

        if (administrator.getRole()
                != UserRole.TENANT_ADMIN) {
            throw new IllegalArgumentException(
                    "Initial tenant administrator "
                            + "must have legacy role "
                            + "TENANT_ADMIN."
            );
        }

        List<AuthorizationRoleResponse> systemRoles =
                authorizationRoleService
                        .initializeDefaultRoles(
                                tenantId
                        );

        Map<String, AuthorizationRoleResponse>
                rolesByCode =
                indexRolesByCode(systemRoles);

        AuthorizationRoleResponse adminRole =
                requireRole(
                        rolesByCode,
                        SystemRoleCodes.ADMIN
                );

        boolean created =
                ensureTenantRoleAssignment(
                        tenantId,
                        administrator,
                        adminRole
                );

        return new AuthorizationProvisioningSummary(
                tenantId,
                systemRoles.size(),
                1,
                created ? 1 : 0,
                created ? 0 : 1,
                0
        );
    }

    /**
     * Safely provisions an existing tenant from legacy UserRole values.
     *
     * This method is idempotent:
     * rerunning it does not create overlapping assignments.
     */
    @Transactional
    public AuthorizationProvisioningSummary
    provisionTenantFromLegacyRoles(
            UUID tenantId
    ) {
        List<AuthorizationRoleResponse> systemRoles =
                authorizationRoleService
                        .initializeDefaultRoles(
                                tenantId
                        );

        Map<String, AuthorizationRoleResponse>
                rolesByCode =
                indexRolesByCode(systemRoles);

        List<AppUser> tenantUsers =
                appUserRepository
                        .findByTenantId(
                                tenantId
                        );

        int assignmentsCreated = 0;
        int assignmentsAlreadyPresent = 0;
        int inactiveUsersSkipped = 0;

        for (AppUser user : tenantUsers) {
            if (user.getStatus() != UserStatus.ACTIVE) {
                inactiveUsersSkipped++;
                continue;
            }

            String systemRoleCode =
                    mapLegacyRoleToSystemRoleCode(
                            user.getRole()
                    );

            AuthorizationRoleResponse role =
                    requireRole(
                            rolesByCode,
                            systemRoleCode
                    );

            boolean created =
                    ensureTenantRoleAssignment(
                            tenantId,
                            user,
                            role
                    );

            if (created) {
                assignmentsCreated++;
            } else {
                assignmentsAlreadyPresent++;
            }
        }

        return new AuthorizationProvisioningSummary(
                tenantId,
                systemRoles.size(),
                tenantUsers.size(),
                assignmentsCreated,
                assignmentsAlreadyPresent,
                inactiveUsersSkipped
        );
    }

    private boolean ensureTenantRoleAssignment(
            UUID tenantId,
            AppUser user,
            AuthorizationRoleResponse role
    ) {
        Instant now =
                normalizeDatabaseInstant(
                        Instant.now()
                );

        long overlappingAssignmentCount =
                authorizationUserRoleAssignmentRepository
                        .countOverlappingActiveAssignments(
                                tenantId,
                                user.getId(),
                                role.id(),
                                AuthorizationScopeType.TENANT,
                                TENANT_SCOPE_KEY,
                                AuthorizationUserRoleAssignmentStatus.ACTIVE,
                                now,
                                null
                        );

        if (overlappingAssignmentCount > 0) {
            return false;
        }

        authorizationUserRoleAssignmentService
                .createAssignment(
                        tenantId,
                        user.getId(),
                        new AuthorizationUserRoleAssignmentCreateRequest(
                                user.getId(),
                                role.id(),
                                AuthorizationScopeType.TENANT,
                                null,
                                now,
                                null
                        )
                );

        return true;
    }

    private String mapLegacyRoleToSystemRoleCode(
            UserRole legacyRole
    ) {
        if (legacyRole == null) {
            throw new IllegalArgumentException(
                    "Legacy user role is required "
                            + "for authorization provisioning."
            );
        }

        return switch (legacyRole) {
            case TENANT_ADMIN ->
                    SystemRoleCodes.ADMIN;

            case TENANT_MANAGER ->
                    SystemRoleCodes.MANAGER;

            case TENANT_USER ->
                    SystemRoleCodes.MEMBER;
        };
    }

    private Map<String, AuthorizationRoleResponse>
    indexRolesByCode(
            List<AuthorizationRoleResponse> roles
    ) {
        return roles.stream()
                .collect(
                        Collectors.toMap(
                                AuthorizationRoleResponse::code,
                                Function.identity()
                        )
                );
    }

    private AuthorizationRoleResponse requireRole(
            Map<String, AuthorizationRoleResponse>
                    rolesByCode,
            String roleCode
    ) {
        AuthorizationRoleResponse role =
                rolesByCode.get(roleCode);

        if (role == null) {
            throw new IllegalStateException(
                    "Required system authorization role "
                            + "was not initialized: "
                            + roleCode
            );
        }

        return role;
    }

    private AppUser getRequiredUser(
            UUID tenantId,
            UUID userId
    ) {
        if (tenantId == null) {
            throw new IllegalArgumentException(
                    "Tenant id is required."
            );
        }

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User id is required."
            );
        }

        return appUserRepository
                .findByTenantIdAndId(
                        tenantId,
                        userId
                )
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant user not found "
                                                + "with id: "
                                                + userId
                                )
                );
    }

    private Instant normalizeDatabaseInstant(
            Instant value
    ) {
        return value.truncatedTo(
                ChronoUnit.MICROS
        );
    }
}