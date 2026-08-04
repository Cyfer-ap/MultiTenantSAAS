package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.OrganizationAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.OrganizationAssignmentService;
import com.chacha.multitenantsaas.service.OrganizationHierarchyService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationAuthorizationV2IntegrationTest {

    private static final String PASSWORD =
            "OrganizationScope@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TenantOnboardingService
            tenantOnboardingService;

    @Autowired
    private AuthorizationRoleService
            authorizationRoleService;

    @Autowired
    private AuthorizationUserRoleAssignmentService
            authorizationAssignmentService;

    @Autowired
    private OrganizationHierarchyService
            organizationHierarchyService;

    @Autowired
    private OrganizationAssignmentService
            organizationAssignmentService;

    @Autowired
    private AuthorizationPermissionRepository
            permissionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void organizationalSubtreeScopeIsIsolated()
            throws Exception {

        TestContext context =
                createContext(
                        "org-subtree"
                );

        AuthorizationRoleResponse role =
                createRole(
                        context.tenant(),
                        "ENGINEERING_OPERATOR",
                        Set.of(
                                PlatformPermissionCodes
                                        .ORGANIZATION_UNIT_READ,
                                PlatformPermissionCodes
                                        .ORGANIZATION_UNIT_MANAGE,
                                PlatformPermissionCodes
                                        .ORGANIZATION_ASSIGNMENT_READ,
                                PlatformPermissionCodes
                                        .ORGANIZATION_ASSIGNMENT_MANAGE
                        )
                );

        assignRole(
                context,
                context.operator(),
                role,
                AuthorizationScopeType
                        .ORGANIZATIONAL_SUBTREE,
                context.engineering().getId()
        );

        String accessToken =
                login(
                        context.tenant().getId(),
                        context.operator().getEmail()
                );

        mockMvc.perform(
                        get(
                                unitUrl("/{unitId}"),
                                context.tenant().getId(),
                                context.backend().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.id")
                                .value(
                                        context.backend()
                                                .getId()
                                                .toString()
                                )
                );

        mockMvc.perform(
                        get(
                                unitUrl("/{unitId}"),
                                context.tenant().getId(),
                                context.finance().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                unitUrl("/{unitId}/subtree"),
                                context.tenant().getId(),
                                context.engineering().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                unitUrl("/{unitId}/subtree"),
                                context.tenant().getId(),
                                context.finance().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                unitUrl("/tree"),
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put(
                                unitUrl("/{unitId}"),
                                context.tenant().getId(),
                                context.backend().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Backend Platform",
                                          "code": "BACKEND",
                                          "type": "DEPARTMENT"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.name")
                                .value("Backend Platform")
                );

        mockMvc.perform(
                        put(
                                unitUrl("/{unitId}"),
                                context.tenant().getId(),
                                context.finance().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Unauthorized Finance",
                                          "code": "FINANCE",
                                          "type": "DEPARTMENT"
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post(
                                assignmentUrl(""),
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        assignmentBody(
                                                context.targetUser()
                                                        .getId(),
                                                context.backend()
                                                        .getId(),
                                                "Backend Engineer"
                                        )
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post(
                                assignmentUrl(""),
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        assignmentBody(
                                                context.otherUser()
                                                        .getId(),
                                                context.finance()
                                                        .getId(),
                                                "Finance Analyst"
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                assignmentUrl(
                                        "/units/{organizationalUnitId}"
                                ),
                                context.tenant().getId(),
                                context.backend().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                assignmentUrl(
                                        "/units/{organizationalUnitId}"
                                ),
                                context.tenant().getId(),
                                context.finance().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void exactUnitScopeCannotReadWholeSubtree()
            throws Exception {

        TestContext context =
                createContext(
                        "org-exact"
                );

        AuthorizationRoleResponse role =
                createRole(
                        context.tenant(),
                        "EXACT_UNIT_READER",
                        Set.of(
                                PlatformPermissionCodes
                                        .ORGANIZATION_UNIT_READ
                        )
                );

        assignRole(
                context,
                context.operator(),
                role,
                AuthorizationScopeType
                        .ORGANIZATIONAL_UNIT,
                context.engineering().getId()
        );

        String accessToken =
                login(
                        context.tenant().getId(),
                        context.operator().getEmail()
                );

        mockMvc.perform(
                        get(
                                unitUrl("/{unitId}"),
                                context.tenant().getId(),
                                context.engineering().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                unitUrl("/{unitId}"),
                                context.tenant().getId(),
                                context.backend().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                unitUrl("/{unitId}/subtree"),
                                context.tenant().getId(),
                                context.engineering().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void directReportsScopeReadsOnlyDirectReports()
            throws Exception {

        TestContext context =
                createContext(
                        "org-reports"
                );

        Instant validFrom =
                Instant.now()
                        .minus(
                                1,
                                ChronoUnit.DAYS
                        )
                        .truncatedTo(
                                ChronoUnit.MICROS
                        );

        UUID managerAssignmentId =
                organizationAssignmentService
                        .createAssignment(
                                context.tenant().getId(),
                                context.administrator().getId(),
                                new OrganizationAssignmentCreateRequest(
                                        context.operator().getId(),
                                        context.engineering().getId(),
                                        null,
                                        "Engineering Manager",
                                        true,
                                        validFrom,
                                        null
                                )
                        )
                        .id();

        UUID directReportAssignmentId =
                organizationAssignmentService
                        .createAssignment(
                                context.tenant().getId(),
                                context.administrator().getId(),
                                new OrganizationAssignmentCreateRequest(
                                        context.targetUser().getId(),
                                        context.backend().getId(),
                                        managerAssignmentId,
                                        "Backend Engineer",
                                        true,
                                        validFrom,
                                        null
                                )
                        )
                        .id();

        UUID unrelatedAssignmentId =
                organizationAssignmentService
                        .createAssignment(
                                context.tenant().getId(),
                                context.administrator().getId(),
                                new OrganizationAssignmentCreateRequest(
                                        context.otherUser().getId(),
                                        context.finance().getId(),
                                        null,
                                        "Finance Analyst",
                                        true,
                                        validFrom,
                                        null
                                )
                        )
                        .id();

        AuthorizationRoleResponse role =
                createRole(
                        context.tenant(),
                        "DIRECT_REPORT_READER",
                        Set.of(
                                PlatformPermissionCodes
                                        .ORGANIZATION_ASSIGNMENT_READ
                        )
                );

        assignRole(
                context,
                context.operator(),
                role,
                AuthorizationScopeType.DIRECT_REPORTS,
                managerAssignmentId
        );

        String accessToken =
                login(
                        context.tenant().getId(),
                        context.operator().getEmail()
                );

        mockMvc.perform(
                        get(
                                assignmentUrl(
                                        "/{managerAssignmentId}"
                                                + "/direct-reports"
                                ),
                                context.tenant().getId(),
                                managerAssignmentId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.length()")
                                .value(1)
                );

        mockMvc.perform(
                        get(
                                assignmentUrl("/{assignmentId}"),
                                context.tenant().getId(),
                                directReportAssignmentId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                assignmentUrl("/{assignmentId}"),
                                context.tenant().getId(),
                                unrelatedAssignmentId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());
    }

    private TestContext createContext(
            String prefix
    ) {
        String suffix = uniqueSuffix();

        TenantOnboardingResponse onboarding =
                tenantOnboardingService
                        .onboardTenant(
                                new TenantOnboardingRequest(
                                        prefix + " Tenant",
                                        prefix + "-" + suffix,
                                        prefix + " Administrator",
                                        prefix
                                                + ".admin."
                                                + suffix
                                                + "@example.test",
                                        PASSWORD
                                )
                        );

        Tenant tenant =
                tenantRepository
                        .findById(
                                onboarding.tenant().id()
                        )
                        .orElseThrow();

        AppUser administrator =
                appUserRepository
                        .findByTenantIdAndId(
                                tenant.getId(),
                                onboarding.adminUser().id()
                        )
                        .orElseThrow();

        /*
         * Deliberately legacy TENANT_ADMIN.
         *
         * Once the V2 role is assigned, this legacy role must
         * not bypass the organizational scope.
         */
        AppUser operator =
                createUser(
                        tenant,
                        prefix + " Operator",
                        UserRole.TENANT_ADMIN
                );

        AppUser targetUser =
                createUser(
                        tenant,
                        prefix + " Target",
                        UserRole.TENANT_USER
                );

        AppUser otherUser =
                createUser(
                        tenant,
                        prefix + " Other",
                        UserRole.TENANT_USER
                );

        OrganizationalUnit engineering =
                createUnit(
                        tenant,
                        null,
                        "Engineering",
                        "ENGINEERING"
                );

        OrganizationalUnit backend =
                createUnit(
                        tenant,
                        engineering.getId(),
                        "Backend",
                        "BACKEND"
                );

        OrganizationalUnit finance =
                createUnit(
                        tenant,
                        null,
                        "Finance",
                        "FINANCE"
                );

        return new TestContext(
                tenant,
                administrator,
                operator,
                targetUser,
                otherUser,
                engineering,
                backend,
                finance
        );
    }

    private AuthorizationRoleResponse createRole(
            Tenant tenant,
            String roleCode,
            Set<String> permissionCodes
    ) {
        Set<UUID> permissionIds =
                permissionCodes
                        .stream()
                        .map(this::getPermissionId)
                        .collect(
                                java.util.stream.Collectors
                                        .toSet()
                        );

        return authorizationRoleService
                .createTenantRole(
                        tenant.getId(),
                        new AuthorizationRoleCreateRequest(
                                roleCode,
                                roleCode,
                                null,
                                permissionIds
                        )
                );
    }

    private void assignRole(
            TestContext context,
            AppUser user,
            AuthorizationRoleResponse role,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId
    ) {
        authorizationAssignmentService
                .createAssignment(
                        context.tenant().getId(),
                        context.administrator().getId(),
                        new AuthorizationUserRoleAssignmentCreateRequest(
                                user.getId(),
                                role.id(),
                                scopeType,
                                scopeTargetId,
                                Instant.now()
                                        .minus(
                                                1,
                                                ChronoUnit.DAYS
                                        )
                                        .truncatedTo(
                                                ChronoUnit.MICROS
                                        ),
                                null
                        )
                );
    }

    private UUID getPermissionId(String code) {
        return permissionRepository
                .findBySourceAndCode(
                        AuthorizationPermissionSource.PLATFORM,
                        code
                )
                .orElseThrow()
                .getId();
    }

    private OrganizationalUnit createUnit(
            Tenant tenant,
            UUID parentUnitId,
            String name,
            String code
    ) {
        return organizationHierarchyService
                .createUnit(
                        tenant.getId(),
                        parentUnitId,
                        name,
                        code,
                        OrganizationalUnitType.DEPARTMENT
                );
    }

    private AppUser createUser(
            Tenant tenant,
            String fullName,
            UserRole legacyRole
    ) {
        String suffix = uniqueSuffix();

        AppUser user = new AppUser(
                tenant,
                fullName,
                "organization."
                        + suffix
                        + "@example.test",
                passwordEncoder.encode(PASSWORD),
                legacyRole
        );

        return appUserRepository
                .saveAndFlush(user);
    }

    private String login(
            UUID tenantId,
            String email
    ) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}"
                                                + "/auth/login",
                                        tenantId
                                )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "email": "%s",
                                                  "password": "%s"
                                                }
                                                """.formatted(
                                                        email,
                                                        PASSWORD
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode body =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return body
                .path("data")
                .path("accessToken")
                .asText();
    }

    private String assignmentBody(
            UUID userId,
            UUID unitId,
            String title
    ) {
        return """
                {
                  "userId": "%s",
                  "organizationalUnitId": "%s",
                  "reportsToAssignmentId": null,
                  "positionTitle": "%s",
                  "primaryAssignment": true,
                  "validFrom": null,
                  "validUntil": null
                }
                """.formatted(
                userId,
                unitId,
                title
        );
    }

    private String unitUrl(String suffix) {
        return "/api/tenants/{tenantId}"
                + "/organization/units"
                + suffix;
    }

    private String assignmentUrl(String suffix) {
        return "/api/tenants/{tenantId}"
                + "/organization/assignments"
                + suffix;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }

    private record TestContext(
            Tenant tenant,
            AppUser administrator,
            AppUser operator,
            AppUser targetUser,
            AppUser otherUser,
            OrganizationalUnit engineering,
            OrganizationalUnit backend,
            OrganizationalUnit finance
    ) {
    }
}