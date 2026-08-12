package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chacha.multitenantsaas.dto.AuthorizationPermissionResponse;
import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.TenantPermissionCreateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import com.chacha.multitenantsaas.service.AuthorizationPermissionEvaluator;
import com.chacha.multitenantsaas.service.AuthorizationPermissionService;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.OrganizationAssignmentService;
import com.chacha.multitenantsaas.service.OrganizationHierarchyService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorizationPermissionEvaluatorIntegrationTest {

    @Autowired private AuthorizationPermissionEvaluator evaluator;

    @Autowired private AuthorizationSecurityService authorizationSecurityService;

    @Autowired private AuthorizationRoleService authorizationRoleService;

    @Autowired private AuthorizationPermissionService authorizationPermissionService;

    @Autowired private AuthorizationUserRoleAssignmentService userRoleAssignmentService;

    @Autowired private OrganizationHierarchyService organizationHierarchyService;

    @Autowired private OrganizationAssignmentService organizationAssignmentService;

    @Autowired private AuthorizationPermissionRepository authorizationPermissionRepository;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private ProjectRepository projectRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantAndSelfScopesAreEvaluated() {
        Tenant tenant = createTenant("evaluator-tenant-self");

        AppUser creator = createUser(tenant, "Tenant Scope Creator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Tenant Scope Member", UserRole.TENANT_USER);

        AppUser anotherUser = createUser(tenant, "Another Tenant User", UserRole.TENANT_USER);

        AuthorizationRoleResponse memberRole = initializeAndGetRole(tenant, SystemRoleCodes.MEMBER);

        Instant validFrom = effectiveStart();

        createRoleAssignment(
                tenant,
                creator,
                member,
                memberRole,
                AuthorizationScopeType.SELF,
                null,
                validFrom,
                null);

        assertTrue(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.USER_READ,
                        AuthorizationEvaluationContext.user(member.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.USER_READ,
                        AuthorizationEvaluationContext.user(anotherUser.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.PROJECT_READ,
                        AuthorizationEvaluationContext.tenant()));

        createRoleAssignment(
                tenant,
                creator,
                member,
                memberRole,
                AuthorizationScopeType.TENANT,
                null,
                validFrom,
                null);

        assertTrue(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.PROJECT_READ,
                        AuthorizationEvaluationContext.tenant()));

        assertTrue(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.USER_READ,
                        AuthorizationEvaluationContext.user(anotherUser.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.AUTHORIZATION_MANAGE,
                        AuthorizationEvaluationContext.tenant()));
    }

    @Test
    void projectScopeRequiresExactActiveProject() {
        Tenant tenant = createTenant("evaluator-project");

        Tenant anotherTenant = createTenant("evaluator-project-other");

        AppUser creator = createUser(tenant, "Project Scope Creator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Project Scope Member", UserRole.TENANT_USER);

        AppUser anotherCreator =
                createUser(anotherTenant, "Other Project Creator", UserRole.TENANT_ADMIN);

        AuthorizationRoleResponse memberRole = initializeAndGetRole(tenant, SystemRoleCodes.MEMBER);

        Project allowedProject = createProject(tenant, creator, "Allowed Project");

        Project anotherProject = createProject(tenant, creator, "Another Project");

        Project crossTenantProject =
                createProject(anotherTenant, anotherCreator, "Cross Tenant Project");

        createRoleAssignment(
                tenant,
                creator,
                member,
                memberRole,
                AuthorizationScopeType.PROJECT,
                allowedProject.getId(),
                effectiveStart(),
                null);

        assertTrue(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.PROJECT_READ,
                        AuthorizationEvaluationContext.project(allowedProject.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.PROJECT_READ,
                        AuthorizationEvaluationContext.project(anotherProject.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.PROJECT_READ,
                        AuthorizationEvaluationContext.project(crossTenantProject.getId())));

        allowedProject.setStatus(ProjectStatus.ARCHIVED);

        projectRepository.saveAndFlush(allowedProject);

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.PROJECT_READ,
                        AuthorizationEvaluationContext.project(allowedProject.getId())));
    }

    @Test
    void exactUnitAndSubtreeScopesAreEvaluated() {
        Tenant tenant = createTenant("evaluator-organization");

        AppUser creator = createUser(tenant, "Organization Scope Creator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Organization Scope Member", UserRole.TENANT_USER);

        AuthorizationRoleResponse memberRole = initializeAndGetRole(tenant, SystemRoleCodes.MEMBER);

        OrganizationalUnit engineering = createUnit(tenant, null, "Engineering", "ENGINEERING");

        OrganizationalUnit backend = createUnit(tenant, engineering.getId(), "Backend", "BACKEND");

        OrganizationalUnit platform = createUnit(tenant, backend.getId(), "Platform", "PLATFORM");

        OrganizationalUnit finance = createUnit(tenant, null, "Finance", "FINANCE");

        createRoleAssignment(
                tenant,
                creator,
                member,
                memberRole,
                AuthorizationScopeType.ORGANIZATIONAL_UNIT,
                engineering.getId(),
                effectiveStart(),
                null);

        assertTrue(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.ORGANIZATION_UNIT_READ,
                        AuthorizationEvaluationContext.organizationalUnit(engineering.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.ORGANIZATION_UNIT_READ,
                        AuthorizationEvaluationContext.organizationalUnit(backend.getId())));

        createRoleAssignment(
                tenant,
                creator,
                member,
                memberRole,
                AuthorizationScopeType.ORGANIZATIONAL_SUBTREE,
                engineering.getId(),
                effectiveStart(),
                null);

        assertTrue(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.ORGANIZATION_UNIT_READ,
                        AuthorizationEvaluationContext.organizationalUnit(backend.getId())));

        assertTrue(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.ORGANIZATION_UNIT_READ,
                        AuthorizationEvaluationContext.organizationalUnit(platform.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        member.getId(),
                        PlatformPermissionCodes.ORGANIZATION_UNIT_READ,
                        AuthorizationEvaluationContext.organizationalUnit(finance.getId())));
    }

    @Test
    void directReportsScopeMatchesOnlyImmediateReports() {
        Tenant tenant = createTenant("evaluator-reports");

        AppUser creator = createUser(tenant, "Reports Scope Creator", UserRole.TENANT_ADMIN);

        AppUser manager = createUser(tenant, "Reports Scope Manager", UserRole.TENANT_MANAGER);

        AppUser directReport = createUser(tenant, "Direct Report", UserRole.TENANT_USER);

        AppUser indirectReport = createUser(tenant, "Indirect Report", UserRole.TENANT_USER);

        AuthorizationRoleResponse managerRole =
                initializeAndGetRole(tenant, SystemRoleCodes.MANAGER);

        OrganizationalUnit operations = createUnit(tenant, null, "Operations", "OPERATIONS");

        Instant validFrom = effectiveStart();

        UUID managerAssignmentId =
                createOrganizationAssignment(
                        tenant,
                        creator,
                        manager,
                        operations,
                        null,
                        "Operations Manager",
                        validFrom);

        UUID directAssignmentId =
                createOrganizationAssignment(
                        tenant,
                        creator,
                        directReport,
                        operations,
                        managerAssignmentId,
                        "Operations Specialist",
                        validFrom);

        createOrganizationAssignment(
                tenant,
                creator,
                indirectReport,
                operations,
                directAssignmentId,
                "Operations Associate",
                validFrom);

        createRoleAssignment(
                tenant,
                creator,
                manager,
                managerRole,
                AuthorizationScopeType.DIRECT_REPORTS,
                managerAssignmentId,
                validFrom,
                null);

        assertTrue(
                evaluator.hasPermission(
                        tenant.getId(),
                        manager.getId(),
                        PlatformPermissionCodes.USER_READ,
                        AuthorizationEvaluationContext.user(directReport.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        manager.getId(),
                        PlatformPermissionCodes.USER_READ,
                        AuthorizationEvaluationContext.user(indirectReport.getId())));

        assertFalse(
                evaluator.hasPermission(
                        tenant.getId(),
                        manager.getId(),
                        PlatformPermissionCodes.USER_READ,
                        AuthorizationEvaluationContext.user(manager.getId())));
    }

    @Test
    void inactiveExpiredAndInactiveRoleGrantsDoNotAuthorize() {
        Tenant tenant = createTenant("evaluator-inactive");

        AppUser creator = createUser(tenant, "Inactive Grant Creator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Inactive Grant Member", UserRole.TENANT_USER);

        AuthorizationRoleResponse firstRole =
                createCustomRole(tenant, "FIRST_VIEWER", PlatformPermissionCodes.PROJECT_READ);

        AuthorizationUserRoleAssignmentResponse firstAssignment =
                createRoleAssignment(
                        tenant,
                        creator,
                        member,
                        firstRole,
                        AuthorizationScopeType.TENANT,
                        null,
                        effectiveStart(),
                        null);

        assertTrue(hasTenantPermission(tenant, member, PlatformPermissionCodes.PROJECT_READ));

        userRoleAssignmentService.deactivateAssignment(tenant.getId(), firstAssignment.id());

        assertFalse(hasTenantPermission(tenant, member, PlatformPermissionCodes.PROJECT_READ));

        AuthorizationRoleResponse expiredRole =
                createCustomRole(tenant, "EXPIRED_VIEWER", PlatformPermissionCodes.PROJECT_READ);

        createRoleAssignment(
                tenant,
                creator,
                member,
                expiredRole,
                AuthorizationScopeType.TENANT,
                null,
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(5, ChronoUnit.DAYS));

        assertFalse(hasTenantPermission(tenant, member, PlatformPermissionCodes.PROJECT_READ));

        AuthorizationRoleResponse inactiveRole =
                createCustomRole(tenant, "INACTIVE_VIEWER", PlatformPermissionCodes.PROJECT_READ);

        createRoleAssignment(
                tenant,
                creator,
                member,
                inactiveRole,
                AuthorizationScopeType.TENANT,
                null,
                effectiveStart(),
                null);

        assertTrue(hasTenantPermission(tenant, member, PlatformPermissionCodes.PROJECT_READ));

        authorizationRoleService.deactivateTenantRole(tenant.getId(), inactiveRole.id());

        assertFalse(hasTenantPermission(tenant, member, PlatformPermissionCodes.PROJECT_READ));
    }

    @Test
    void inactiveCustomPermissionDoesNotAuthorize() {
        Tenant tenant = createTenant("evaluator-custom-permission");

        AppUser creator = createUser(tenant, "Custom Permission Creator", UserRole.TENANT_ADMIN);

        AppUser member = createUser(tenant, "Custom Permission Member", UserRole.TENANT_USER);

        AuthorizationPermissionResponse customPermission =
                authorizationPermissionService.createTenantPermission(
                        tenant.getId(),
                        new TenantPermissionCreateRequest(
                                "custom.report.export", "Export reports", null, "REPORTING"));

        AuthorizationRoleResponse customRole =
                authorizationRoleService.createTenantRole(
                        tenant.getId(),
                        new AuthorizationRoleCreateRequest(
                                "REPORT_EXPORTER",
                                "Report Exporter",
                                null,
                                Set.of(customPermission.id())));

        createRoleAssignment(
                tenant,
                creator,
                member,
                customRole,
                AuthorizationScopeType.TENANT,
                null,
                effectiveStart(),
                null);

        assertTrue(hasTenantPermission(tenant, member, "custom.report.export"));

        authorizationPermissionService.deactivateTenantPermission(
                tenant.getId(), customPermission.id());

        assertFalse(hasTenantPermission(tenant, member, "custom.report.export"));
    }

    @Test
    void springSecurityBeanUsesJwtIdentityAndDatabaseRoles() {
        Tenant tenant = createTenant("evaluator-security-bean");

        Tenant anotherTenant = createTenant("evaluator-security-bean-other");

        AppUser user = createUser(tenant, "Database Authorization User", UserRole.TENANT_USER);

        AuthorizationRoleResponse adminRole = initializeAndGetRole(tenant, SystemRoleCodes.ADMIN);

        createRoleAssignment(
                tenant,
                user,
                user,
                adminRole,
                AuthorizationScopeType.TENANT,
                null,
                effectiveStart(),
                null);

        setAuthenticatedJwt(user);

        assertTrue(
                authorizationSecurityService.hasTenantPermission(
                        tenant.getId(), PlatformPermissionCodes.AUTHORIZATION_MANAGE));

        assertFalse(
                authorizationSecurityService.hasTenantPermission(
                        anotherTenant.getId(), PlatformPermissionCodes.AUTHORIZATION_MANAGE));
    }

    private boolean hasTenantPermission(Tenant tenant, AppUser user, String permissionCode) {
        return evaluator.hasPermission(
                tenant.getId(),
                user.getId(),
                permissionCode,
                AuthorizationEvaluationContext.tenant());
    }

    private AuthorizationUserRoleAssignmentResponse createRoleAssignment(
            Tenant tenant,
            AppUser createdBy,
            AppUser assignedUser,
            AuthorizationRoleResponse role,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId,
            Instant validFrom,
            Instant validUntil) {
        return userRoleAssignmentService.createAssignment(
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

    private AuthorizationRoleResponse createCustomRole(
            Tenant tenant, String roleCode, String permissionCode) {
        AuthorizationPermission permission =
                authorizationPermissionRepository
                        .findBySourceAndCode(AuthorizationPermissionSource.PLATFORM, permissionCode)
                        .orElseThrow();

        return authorizationRoleService.createTenantRole(
                tenant.getId(),
                new AuthorizationRoleCreateRequest(
                        roleCode, roleCode, null, Set.of(permission.getId())));
    }

    private UUID createOrganizationAssignment(
            Tenant tenant,
            AppUser createdBy,
            AppUser assignedUser,
            OrganizationalUnit unit,
            UUID reportsToAssignmentId,
            String positionTitle,
            Instant validFrom) {
        return organizationAssignmentService
                .createAssignment(
                        tenant.getId(),
                        createdBy.getId(),
                        new OrganizationAssignmentCreateRequest(
                                assignedUser.getId(),
                                unit.getId(),
                                reportsToAssignmentId,
                                positionTitle,
                                true,
                                validFrom,
                                null))
                .id();
    }

    private OrganizationalUnit createUnit(
            Tenant tenant, UUID parentUnitId, String name, String code) {
        return organizationHierarchyService.createUnit(
                tenant.getId(), parentUnitId, name, code, OrganizationalUnitType.DEPARTMENT);
    }

    private Project createProject(Tenant tenant, AppUser createdBy, String name) {
        Project project =
                new Project(tenant, createdBy, name, "Authorization evaluator test project");

        return projectRepository.saveAndFlush(project);
    }

    private AppUser createUser(Tenant tenant, String fullName, UserRole legacyRole) {
        AppUser user =
                new AppUser(
                        tenant,
                        fullName,
                        legacyRole.name().toLowerCase().replace('_', '.')
                                + "."
                                + uniqueSuffix()
                                + "@example.test",
                        "test-password-hash",
                        legacyRole);

        return appUserRepository.saveAndFlush(user);
    }

    private Tenant createTenant(String prefix) {
        String suffix = uniqueSuffix();

        Tenant tenant = new Tenant(prefix + " Tenant", prefix + "-" + suffix);

        return tenantRepository.saveAndFlush(tenant);
    }

    private Instant effectiveStart() {
        return Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    }

    private void setAuthenticatedJwt(AppUser user) {
        Instant now = Instant.now();

        Jwt jwt =
                Jwt.withTokenValue("authorization-evaluator-test")
                        .header("alg", "none")
                        .subject(user.getId().toString())
                        .claim("tenantId", user.getTenant().getId().toString())
                        .claim("email", user.getEmail())
                        .claim("fullName", user.getFullName())
                        .claim("role", user.getRole().name())
                        .issuedAt(now)
                        .expiresAt(now.plus(1, ChronoUnit.HOURS))
                        .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
