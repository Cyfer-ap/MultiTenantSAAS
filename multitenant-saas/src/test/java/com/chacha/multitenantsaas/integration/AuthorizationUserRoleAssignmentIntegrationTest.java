package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentCreateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.AuthorizationUserRoleAssignmentStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.OrganizationAssignmentService;
import com.chacha.multitenantsaas.service.OrganizationHierarchyService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorizationUserRoleAssignmentIntegrationTest {

    @Autowired private AuthorizationUserRoleAssignmentService assignmentService;

    @Autowired private AuthorizationRoleService authorizationRoleService;

    @Autowired private OrganizationHierarchyService organizationHierarchyService;

    @Autowired private OrganizationAssignmentService organizationAssignmentService;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private ProjectRepository projectRepository;

    @Test
    void createsTenantSelfOrganizationAndProjectScopes() {
        Tenant tenant = createTenant("role-scope-create");

        AppUser administrator = createUser(tenant, "Scope Administrator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Scope Member", UserRole.TENANT_USER);

        AuthorizationRoleResponse memberRole = initializeAndGetRole(tenant, SystemRoleCodes.MEMBER);

        OrganizationalUnit unit = createUnit(tenant, "Engineering", "ENGINEERING");

        Project project = createProject(tenant, administrator, "Authorization Project");

        Instant validFrom = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

        AuthorizationUserRoleAssignmentResponse tenantScope =
                createAssignment(
                        tenant,
                        administrator,
                        member,
                        memberRole,
                        AuthorizationScopeType.TENANT,
                        null,
                        validFrom,
                        null);

        AuthorizationUserRoleAssignmentResponse selfScope =
                createAssignment(
                        tenant,
                        administrator,
                        member,
                        memberRole,
                        AuthorizationScopeType.SELF,
                        null,
                        validFrom,
                        null);

        AuthorizationUserRoleAssignmentResponse unitScope =
                createAssignment(
                        tenant,
                        administrator,
                        member,
                        memberRole,
                        AuthorizationScopeType.ORGANIZATIONAL_UNIT,
                        unit.getId(),
                        validFrom,
                        null);

        AuthorizationUserRoleAssignmentResponse subtreeScope =
                createAssignment(
                        tenant,
                        administrator,
                        member,
                        memberRole,
                        AuthorizationScopeType.ORGANIZATIONAL_SUBTREE,
                        unit.getId(),
                        validFrom,
                        null);

        AuthorizationUserRoleAssignmentResponse projectScope =
                createAssignment(
                        tenant,
                        administrator,
                        member,
                        memberRole,
                        AuthorizationScopeType.PROJECT,
                        project.getId(),
                        validFrom,
                        null);

        assertNotNull(tenantScope.id());
        assertNotNull(selfScope.id());

        assertEquals(unit.getId(), unitScope.scopeTargetId());

        assertEquals(unit.getId(), subtreeScope.scopeTargetId());

        assertEquals(project.getId(), projectScope.scopeTargetId());

        List<AuthorizationUserRoleAssignmentResponse> assignments =
                assignmentService.getUserAssignments(tenant.getId(), member.getId());

        assertEquals(5, assignments.size());
    }

    @Test
    void rejectsOverlappingAndAllowsSequentialAssignment() {
        Tenant tenant = createTenant("role-scope-overlap");

        AppUser administrator = createUser(tenant, "Overlap Administrator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Overlap Member", UserRole.TENANT_USER);

        AuthorizationRoleResponse memberRole = initializeAndGetRole(tenant, SystemRoleCodes.MEMBER);

        Instant validFrom = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

        Instant validUntil = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

        createAssignment(
                tenant,
                administrator,
                member,
                memberRole,
                AuthorizationScopeType.TENANT,
                null,
                validFrom,
                validUntil);

        assertThrows(
                DuplicateResourceException.class,
                () ->
                        createAssignment(
                                tenant,
                                administrator,
                                member,
                                memberRole,
                                AuthorizationScopeType.TENANT,
                                null,
                                Instant.now(),
                                null));

        AuthorizationUserRoleAssignmentResponse sequential =
                createAssignment(
                        tenant,
                        administrator,
                        member,
                        memberRole,
                        AuthorizationScopeType.TENANT,
                        null,
                        validUntil,
                        null);

        assertEquals(validUntil, sequential.validFrom());
    }

    @Test
    void directReportsScopeRequiresUsersOwnAssignment() {
        Tenant tenant = createTenant("role-scope-reports");

        AppUser administrator = createUser(tenant, "Reports Administrator", UserRole.TENANT_ADMIN);

        AppUser manager = createUser(tenant, "Reports Manager", UserRole.TENANT_MANAGER);

        AppUser otherManager = createUser(tenant, "Other Manager", UserRole.TENANT_MANAGER);

        AuthorizationRoleResponse managerRole =
                initializeAndGetRole(tenant, SystemRoleCodes.MANAGER);

        OrganizationalUnit unit = createUnit(tenant, "Operations", "OPERATIONS");

        Instant validFrom = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

        UUID managerOrganizationAssignmentId =
                organizationAssignmentService
                        .createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                new OrganizationAssignmentCreateRequest(
                                        manager.getId(),
                                        unit.getId(),
                                        null,
                                        "Operations Manager",
                                        true,
                                        validFrom,
                                        null))
                        .id();

        UUID otherOrganizationAssignmentId =
                organizationAssignmentService
                        .createAssignment(
                                tenant.getId(),
                                administrator.getId(),
                                new OrganizationAssignmentCreateRequest(
                                        otherManager.getId(),
                                        unit.getId(),
                                        null,
                                        "Other Manager",
                                        true,
                                        validFrom,
                                        null))
                        .id();

        AuthorizationUserRoleAssignmentResponse directReportsAssignment =
                createAssignment(
                        tenant,
                        administrator,
                        manager,
                        managerRole,
                        AuthorizationScopeType.DIRECT_REPORTS,
                        managerOrganizationAssignmentId,
                        validFrom,
                        null);

        assertEquals(managerOrganizationAssignmentId, directReportsAssignment.scopeTargetId());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        createAssignment(
                                tenant,
                                administrator,
                                manager,
                                managerRole,
                                AuthorizationScopeType.DIRECT_REPORTS,
                                otherOrganizationAssignmentId,
                                validFrom,
                                null));
    }

    @Test
    void rejectsCrossTenantUsersRolesAndTargets() {
        Tenant firstTenant = createTenant("role-scope-first");

        Tenant secondTenant = createTenant("role-scope-second");

        AppUser firstAdministrator =
                createUser(firstTenant, "First Scope Administrator", UserRole.TENANT_ADMIN);

        AppUser firstMember = createUser(firstTenant, "First Scope Member", UserRole.TENANT_USER);

        AppUser secondAdministrator =
                createUser(secondTenant, "Second Scope Administrator", UserRole.TENANT_ADMIN);

        AppUser secondMember =
                createUser(secondTenant, "Second Scope Member", UserRole.TENANT_USER);

        AuthorizationRoleResponse firstRole =
                initializeAndGetRole(firstTenant, SystemRoleCodes.MEMBER);

        AuthorizationRoleResponse secondRole =
                initializeAndGetRole(secondTenant, SystemRoleCodes.MEMBER);

        OrganizationalUnit secondUnit = createUnit(secondTenant, "Second Unit", "SECOND-UNIT");

        Project secondProject = createProject(secondTenant, secondAdministrator, "Second Project");

        Instant validFrom = Instant.now().truncatedTo(ChronoUnit.MICROS);

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        createAssignment(
                                firstTenant,
                                firstAdministrator,
                                secondMember,
                                firstRole,
                                AuthorizationScopeType.TENANT,
                                null,
                                validFrom,
                                null));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        createAssignment(
                                firstTenant,
                                firstAdministrator,
                                firstMember,
                                secondRole,
                                AuthorizationScopeType.TENANT,
                                null,
                                validFrom,
                                null));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        createAssignment(
                                firstTenant,
                                firstAdministrator,
                                firstMember,
                                firstRole,
                                AuthorizationScopeType.ORGANIZATIONAL_UNIT,
                                secondUnit.getId(),
                                validFrom,
                                null));

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        createAssignment(
                                firstTenant,
                                firstAdministrator,
                                firstMember,
                                firstRole,
                                AuthorizationScopeType.PROJECT,
                                secondProject.getId(),
                                validFrom,
                                null));
    }

    @Test
    void effectiveQueryAndDeactivationRespectAssignmentStatus() {
        Tenant tenant = createTenant("role-scope-effective");

        AppUser administrator =
                createUser(tenant, "Effective Administrator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Effective Member", UserRole.TENANT_USER);

        AuthorizationRoleResponse memberRole = initializeAndGetRole(tenant, SystemRoleCodes.MEMBER);

        Instant validFrom = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

        AuthorizationUserRoleAssignmentResponse assignment =
                createAssignment(
                        tenant,
                        administrator,
                        member,
                        memberRole,
                        AuthorizationScopeType.SELF,
                        null,
                        validFrom,
                        null);

        List<AuthorizationUserRoleAssignmentResponse> effectiveBefore =
                assignmentService.getEffectiveUserAssignments(
                        tenant.getId(), member.getId(), Instant.now());

        assertEquals(1, effectiveBefore.size());

        AuthorizationUserRoleAssignmentResponse deactivated =
                assignmentService.deactivateAssignment(tenant.getId(), assignment.id());

        assertEquals(AuthorizationUserRoleAssignmentStatus.INACTIVE, deactivated.status());

        assertNotNull(deactivated.validUntil());

        List<AuthorizationUserRoleAssignmentResponse> effectiveAfter =
                assignmentService.getEffectiveUserAssignments(
                        tenant.getId(), member.getId(), Instant.now());

        assertTrue(effectiveAfter.isEmpty());

        List<AuthorizationUserRoleAssignmentResponse> allAssignments =
                assignmentService.getUserAssignments(tenant.getId(), member.getId());

        assertEquals(1, allAssignments.size());

        assertFalse(
                allAssignments.getFirst().status() == AuthorizationUserRoleAssignmentStatus.ACTIVE);
    }

    private AuthorizationUserRoleAssignmentResponse createAssignment(
            Tenant tenant,
            AppUser createdBy,
            AppUser assignedUser,
            AuthorizationRoleResponse role,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId,
            Instant validFrom,
            Instant validUntil) {
        return assignmentService.createAssignment(
                tenant.getId(),
                createdBy.getId(),
                new AuthorizationUserRoleAssignmentCreateRequest(
                        assignedUser.getId(),
                        role.id(),
                        scopeType,
                        scopeTargetId,
                        validFrom,
                        validUntil));
    }

    private AuthorizationRoleResponse initializeAndGetRole(Tenant tenant, String roleCode) {
        authorizationRoleService.initializeDefaultRoles(tenant.getId());

        return authorizationRoleService.getRoleByCode(tenant.getId(), roleCode);
    }

    private OrganizationalUnit createUnit(Tenant tenant, String name, String code) {
        return organizationHierarchyService.createUnit(
                tenant.getId(), null, name, code, OrganizationalUnitType.DEPARTMENT);
    }

    private Project createProject(Tenant tenant, AppUser createdBy, String name) {
        Project project = new Project(tenant, createdBy, name, "Authorization scope test project");

        return projectRepository.saveAndFlush(project);
    }

    private AppUser createUser(Tenant tenant, String fullName, UserRole role) {
        AppUser user =
                new AppUser(
                        tenant,
                        fullName,
                        role.name().toLowerCase().replace('_', '.')
                                + "."
                                + uniqueSuffix()
                                + "@example.test",
                        "test-password-hash",
                        role);

        return appUserRepository.saveAndFlush(user);
    }

    private Tenant createTenant(String prefix) {
        String suffix = uniqueSuffix();

        Tenant tenant = new Tenant(prefix + " Tenant", prefix + "-" + suffix);

        return tenantRepository.saveAndFlush(tenant);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
