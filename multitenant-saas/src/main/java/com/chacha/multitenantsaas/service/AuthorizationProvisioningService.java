package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AuthorizationProvisioningIssueResponse;
import com.chacha.multitenantsaas.dto.AuthorizationProvisioningReadinessResponse;
import com.chacha.multitenantsaas.dto.AuthorizationProvisioningSummary;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationRoleSource;
import com.chacha.multitenantsaas.entity.AuthorizationRoleStatus;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignmentStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationUserRoleAssignmentRepository;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationProvisioningService {

    private static final String TENANT_SCOPE_KEY = "TENANT";

    private static final Set<String> REQUIRED_SYSTEM_ROLES =
            Set.of(SystemRoleCodes.ADMIN, SystemRoleCodes.MANAGER, SystemRoleCodes.MEMBER);

    private final AppUserRepository appUserRepository;

    private final AuthorizationRoleService authorizationRoleService;

    private final AuthorizationUserRoleAssignmentService authorizationUserRoleAssignmentService;

    private final AuthorizationUserRoleAssignmentRepository
            authorizationUserRoleAssignmentRepository;

    public AuthorizationProvisioningService(
            AppUserRepository appUserRepository,
            AuthorizationRoleService authorizationRoleService,
            AuthorizationUserRoleAssignmentService authorizationUserRoleAssignmentService,
            AuthorizationUserRoleAssignmentRepository authorizationUserRoleAssignmentRepository) {
        this.appUserRepository = appUserRepository;

        this.authorizationRoleService = authorizationRoleService;

        this.authorizationUserRoleAssignmentService = authorizationUserRoleAssignmentService;

        this.authorizationUserRoleAssignmentRepository = authorizationUserRoleAssignmentRepository;
    }

    @Transactional
    public AuthorizationProvisioningSummary provisionInitialTenantAdministrator(
            UUID tenantId, UUID administratorUserId) {
        AppUser administrator = getRequiredUser(tenantId, administratorUserId);

        if (administrator.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Initial tenant administrator " + "must be active.");
        }

        if (administrator.getRole() != UserRole.TENANT_ADMIN) {
            throw new IllegalArgumentException(
                    "Initial tenant administrator " + "must have legacy role " + "TENANT_ADMIN.");
        }

        List<AuthorizationRoleResponse> systemRoles =
                authorizationRoleService.initializeDefaultRoles(tenantId);

        Map<String, AuthorizationRoleResponse> rolesByCode = indexRolesByCode(systemRoles);

        boolean created = synchronizeActiveUser(tenantId, administrator, rolesByCode);

        return new AuthorizationProvisioningSummary(
                tenantId, systemRoles.size(), 1, created ? 1 : 0, created ? 0 : 1, 0);
    }

    /**
     * Synchronizes one user after creation, a legacy-role change, activation, suspension, or
     * deactivation.
     *
     * <p>Only tenant-wide SYSTEM role assignments are managed. Tenant-custom roles and narrower
     * scoped assignments are intentionally preserved.
     */
    @Transactional
    public void synchronizeUserFromLegacyState(UUID tenantId, UUID userId) {
        AppUser user = getRequiredUser(tenantId, userId);

        Map<String, AuthorizationRoleResponse> rolesByCode = getOrInitializeSystemRoles(tenantId);

        if (user.getStatus() != UserStatus.ACTIVE) {
            deactivateManagedSystemAssignments(tenantId, user.getId(), null);

            return;
        }

        synchronizeActiveUser(tenantId, user, rolesByCode);
    }

    @Transactional
    public AuthorizationProvisioningSummary provisionTenantFromLegacyRoles(UUID tenantId) {
        List<AuthorizationRoleResponse> systemRoles =
                authorizationRoleService.initializeDefaultRoles(tenantId);

        Map<String, AuthorizationRoleResponse> rolesByCode = indexRolesByCode(systemRoles);

        List<AppUser> tenantUsers = appUserRepository.findByTenantId(tenantId);

        int assignmentsCreated = 0;
        int assignmentsAlreadyPresent = 0;
        int inactiveUsersSkipped = 0;

        for (AppUser user : tenantUsers) {
            if (user.getStatus() != UserStatus.ACTIVE) {
                deactivateManagedSystemAssignments(tenantId, user.getId(), null);

                inactiveUsersSkipped++;
                continue;
            }

            boolean created = synchronizeActiveUser(tenantId, user, rolesByCode);

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
                inactiveUsersSkipped);
    }

    @Transactional(readOnly = true)
    public AuthorizationProvisioningReadinessResponse getTenantReadiness(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id is required.");
        }

        List<AuthorizationRoleResponse> activeSystemRoles =
                authorizationRoleService.getRoles(tenantId).stream()
                        .filter(role -> role.source() == AuthorizationRoleSource.SYSTEM)
                        .filter(role -> role.status() == AuthorizationRoleStatus.ACTIVE)
                        .filter(role -> REQUIRED_SYSTEM_ROLES.contains(role.code()))
                        .toList();

        Set<String> availableRoleCodes =
                activeSystemRoles.stream()
                        .map(AuthorizationRoleResponse::code)
                        .collect(Collectors.toSet());

        List<String> missingSystemRoleCodes =
                REQUIRED_SYSTEM_ROLES.stream()
                        .filter(roleCode -> !availableRoleCodes.contains(roleCode))
                        .sorted()
                        .toList();

        List<AppUser> tenantUsers = appUserRepository.findByTenantId(tenantId);

        List<AuthorizationProvisioningIssueResponse> issues = new ArrayList<>();

        int activeUsers = 0;
        int inactiveUsers = 0;
        int compliantUsers = 0;

        for (AppUser user : tenantUsers) {
            List<AuthorizationUserRoleAssignmentResponse> managedAssignments =
                    getActiveManagedSystemAssignments(tenantId, user.getId());

            List<String> activeRoleCodes =
                    managedAssignments.stream()
                            .map(AuthorizationUserRoleAssignmentResponse::roleCode)
                            .sorted()
                            .toList();

            String expectedRoleCode = mapLegacyRoleToSystemRoleCode(user.getRole());

            if (user.getStatus() == UserStatus.ACTIVE) {
                activeUsers++;

                boolean exactlyOneCorrectRole =
                        managedAssignments.size() == 1
                                && expectedRoleCode.equals(
                                        managedAssignments.getFirst().roleCode());

                if (exactlyOneCorrectRole) {
                    compliantUsers++;
                    continue;
                }

                issues.add(
                        new AuthorizationProvisioningIssueResponse(
                                user.getId(),
                                user.getEmail(),
                                user.getRole(),
                                user.getStatus(),
                                expectedRoleCode,
                                activeRoleCodes,
                                buildActiveUserIssueReason(expectedRoleCode, activeRoleCodes)));

                continue;
            }

            inactiveUsers++;

            if (managedAssignments.isEmpty()) {
                compliantUsers++;
                continue;
            }

            issues.add(
                    new AuthorizationProvisioningIssueResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getRole(),
                            user.getStatus(),
                            expectedRoleCode,
                            activeRoleCodes,
                            "Inactive user has active generated " + "system-role assignments."));
        }

        boolean ready = missingSystemRoleCodes.isEmpty() && issues.isEmpty();

        return new AuthorizationProvisioningReadinessResponse(
                tenantId,
                ready,
                activeSystemRoles.size(),
                missingSystemRoleCodes,
                tenantUsers.size(),
                activeUsers,
                inactiveUsers,
                compliantUsers,
                issues.size(),
                List.copyOf(issues));
    }

    private List<AuthorizationUserRoleAssignmentResponse> getActiveManagedSystemAssignments(
            UUID tenantId, UUID userId) {
        return authorizationUserRoleAssignmentService.getUserAssignments(tenantId, userId).stream()
                .filter(assignment -> assignment.roleSource() == AuthorizationRoleSource.SYSTEM)
                .filter(assignment -> assignment.scopeType() == AuthorizationScopeType.TENANT)
                .filter(
                        assignment ->
                                assignment.status() == AuthorizationUserRoleAssignmentStatus.ACTIVE)
                .sorted(
                        Comparator.comparing(AuthorizationUserRoleAssignmentResponse::roleCode)
                                .thenComparing(AuthorizationUserRoleAssignmentResponse::id))
                .toList();
    }

    private boolean synchronizeActiveUser(
            UUID tenantId, AppUser user, Map<String, AuthorizationRoleResponse> rolesByCode) {
        String requiredRoleCode = mapLegacyRoleToSystemRoleCode(user.getRole());

        AuthorizationRoleResponse requiredRole = requireRole(rolesByCode, requiredRoleCode);

        deactivateManagedSystemAssignments(tenantId, user.getId(), requiredRoleCode);

        return ensureTenantRoleAssignment(tenantId, user, requiredRole);
    }

    /**
     * Deactivates generated tenant-wide system grants except the role code that should remain
     * active.
     *
     * <p>When roleCodeToKeep is null, all generated system grants are deactivated.
     */
    private void deactivateManagedSystemAssignments(
            UUID tenantId, UUID userId, String roleCodeToKeep) {
        List<AuthorizationUserRoleAssignmentResponse> assignments =
                authorizationUserRoleAssignmentService.getUserAssignments(tenantId, userId);

        boolean requiredRoleAlreadyKept = false;

        for (AuthorizationUserRoleAssignmentResponse assignment : assignments) {
            boolean managedSystemAssignment =
                    assignment.roleSource() == AuthorizationRoleSource.SYSTEM
                            && assignment.scopeType() == AuthorizationScopeType.TENANT
                            && assignment.status() == AuthorizationUserRoleAssignmentStatus.ACTIVE;

            if (!managedSystemAssignment) {
                continue;
            }

            boolean matchesRequiredRole =
                    roleCodeToKeep != null && roleCodeToKeep.equals(assignment.roleCode());

            if (matchesRequiredRole && !requiredRoleAlreadyKept) {
                requiredRoleAlreadyKept = true;
                continue;
            }

            authorizationUserRoleAssignmentService.deactivateAssignment(tenantId, assignment.id());
        }
    }

    private boolean ensureTenantRoleAssignment(
            UUID tenantId, AppUser user, AuthorizationRoleResponse role) {
        Instant now = normalizeDatabaseInstant(Instant.now());

        long overlappingAssignmentCount =
                authorizationUserRoleAssignmentRepository.countOverlappingActiveAssignments(
                        tenantId,
                        user.getId(),
                        role.id(),
                        AuthorizationScopeType.TENANT,
                        TENANT_SCOPE_KEY,
                        AuthorizationUserRoleAssignmentStatus.ACTIVE,
                        now,
                        null);

        if (overlappingAssignmentCount > 0) {
            return false;
        }

        authorizationUserRoleAssignmentService.createAssignment(
                tenantId,
                user.getId(),
                new AuthorizationUserRoleAssignmentCreateRequest(
                        user.getId(), role.id(), AuthorizationScopeType.TENANT, null, now, null));

        return true;
    }

    private Map<String, AuthorizationRoleResponse> getOrInitializeSystemRoles(UUID tenantId) {
        List<AuthorizationRoleResponse> existingRoles =
                authorizationRoleService.getRoles(tenantId).stream()
                        .filter(role -> role.source() == AuthorizationRoleSource.SYSTEM)
                        .filter(role -> role.status() == AuthorizationRoleStatus.ACTIVE)
                        .toList();

        Set<String> existingCodes =
                existingRoles.stream()
                        .map(AuthorizationRoleResponse::code)
                        .collect(Collectors.toSet());

        if (existingCodes.containsAll(REQUIRED_SYSTEM_ROLES)) {
            return indexRolesByCode(existingRoles);
        }

        return indexRolesByCode(authorizationRoleService.initializeDefaultRoles(tenantId));
    }

    private String mapLegacyRoleToSystemRoleCode(UserRole legacyRole) {
        if (legacyRole == null) {
            throw new IllegalArgumentException(
                    "Legacy user role is required " + "for authorization provisioning.");
        }

        return switch (legacyRole) {
            case TENANT_ADMIN -> SystemRoleCodes.ADMIN;

            case TENANT_MANAGER -> SystemRoleCodes.MANAGER;

            case TENANT_USER -> SystemRoleCodes.MEMBER;
        };
    }

    private Map<String, AuthorizationRoleResponse> indexRolesByCode(
            List<AuthorizationRoleResponse> roles) {
        return roles.stream()
                .collect(Collectors.toMap(AuthorizationRoleResponse::code, Function.identity()));
    }

    private AuthorizationRoleResponse requireRole(
            Map<String, AuthorizationRoleResponse> rolesByCode, String roleCode) {
        AuthorizationRoleResponse role = rolesByCode.get(roleCode);

        if (role == null) {
            throw new IllegalStateException(
                    "Required system authorization role " + "was not initialized: " + roleCode);
        }

        return role;
    }

    private AppUser getRequiredUser(UUID tenantId, UUID userId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id is required.");
        }

        if (userId == null) {
            throw new IllegalArgumentException("User id is required.");
        }

        return appUserRepository
                .findByTenantIdAndId(tenantId, userId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant user not found " + "with id: " + userId));
    }

    private Instant normalizeDatabaseInstant(Instant value) {
        return value.truncatedTo(ChronoUnit.MICROS);
    }

    private String buildActiveUserIssueReason(
            String expectedRoleCode, List<String> activeRoleCodes) {
        if (activeRoleCodes.isEmpty()) {
            return "Active user is missing generated " + "system role " + expectedRoleCode + ".";
        }

        long matchingRoleCount = activeRoleCodes.stream().filter(expectedRoleCode::equals).count();

        if (matchingRoleCount == 0) {
            return "Active user has incorrect generated "
                    + "system role. Expected "
                    + expectedRoleCode
                    + ".";
        }

        if (matchingRoleCount > 1) {
            return "Active user has duplicate generated " + expectedRoleCode + " assignments.";
        }

        return "Active user has additional generated " + "system-role assignments.";
    }
}
