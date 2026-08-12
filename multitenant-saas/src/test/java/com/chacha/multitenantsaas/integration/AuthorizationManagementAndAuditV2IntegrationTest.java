package com.chacha.multitenantsaas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationManagementAndAuditV2IntegrationTest {

    private static final String PASSWORD = "DelegatedAccess@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Autowired private TenantOnboardingService tenantOnboardingService;

    @Autowired private AuthorizationRoleService authorizationRoleService;

    @Autowired private AuthorizationUserRoleAssignmentService assignmentService;

    @Autowired private AuthorizationPermissionRepository permissionRepository;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void delegatedAuthorizationManagerCanUseManagementApi() throws Exception {

        TestContext context = createContext("delegated-authz");

        AppUser delegatedManager =
                createUser(
                        context.tenant(), "Delegated Authorization Manager", UserRole.TENANT_USER);

        AuthorizationRoleResponse managementRole =
                createRole(
                        context.tenant(),
                        "DELEGATED_AUTHORIZATION_MANAGER",
                        Set.of(PlatformPermissionCodes.AUTHORIZATION_MANAGE));

        assignRole(context, delegatedManager, managementRole, AuthorizationScopeType.TENANT, null);

        String accessToken = login(context.tenant().getId(), delegatedManager.getEmail());

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}" + "/authorization/permissions",
                                        context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String customPermissionCode = "custom.delegated.p" + uniqueSuffix();

        mockMvc.perform(
                        post(
                                        "/api/tenants/{tenantId}"
                                                + "/authorization"
                                                + "/permissions/custom",
                                        context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "code": "%s",
                                          "name":
                                            "Delegated permission",
                                          "description":
                                            "Created by a delegated authorization manager.",
                                          "category":
                                            "DELEGATED_TEST"
                                        }
                                        """
                                                .formatted(customPermissionCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(customPermissionCode));

        /*
         * authorization.manage does not automatically grant
         * audit.read.
         */
        mockMvc.perform(
                        get("/api/tenants/{tenantId}" + "/audit-logs", context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delegatedAuditorCanReadAuditButNotManageRoles() throws Exception {

        TestContext context = createContext("delegated-auditor");

        AppUser auditor =
                createUser(context.tenant(), "Delegated Tenant Auditor", UserRole.TENANT_USER);

        AuthorizationRoleResponse auditRole =
                createRole(
                        context.tenant(),
                        "TENANT_AUDITOR",
                        Set.of(PlatformPermissionCodes.AUDIT_READ));

        assignRole(context, auditor, auditRole, AuthorizationScopeType.TENANT, null);

        String accessToken = login(context.tenant().getId(), auditor.getEmail());

        mockMvc.perform(
                        get("/api/tenants/{tenantId}" + "/audit-logs", context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                                .param("page", "0")
                                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}" + "/authorization/roles",
                                        context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void selfScopedAuditorReadsOnlyOwnAuditLogs() throws Exception {

        TestContext context = createContext("self-auditor");

        AppUser selfAuditor =
                createUser(context.tenant(), "Self Audit Reader", UserRole.TENANT_USER);

        AppUser anotherUser =
                createUser(context.tenant(), "Another Audit User", UserRole.TENANT_USER);

        AuthorizationRoleResponse selfAuditRole =
                createRole(
                        context.tenant(),
                        "SELF_AUDIT_READER",
                        Set.of(PlatformPermissionCodes.AUDIT_READ));

        assignRole(context, selfAuditor, selfAuditRole, AuthorizationScopeType.SELF, null);

        /*
         * These logins also create user-specific audit
         * records through the existing authentication flow.
         */
        String selfAuditorToken = login(context.tenant().getId(), selfAuditor.getEmail());

        login(context.tenant().getId(), anotherUser.getEmail());

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}" + "/audit-logs/users/{userId}",
                                        context.tenant().getId(),
                                        selfAuditor.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(selfAuditorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}" + "/audit-logs/users/{userId}",
                                        context.tenant().getId(),
                                        anotherUser.getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(selfAuditorToken)))
                .andExpect(status().isForbidden());

        /*
         * SELF scope cannot read the complete tenant feed.
         */
        mockMvc.perform(
                        get("/api/tenants/{tenantId}" + "/audit-logs", context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(selfAuditorToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void v2AssignmentPreventsLegacyAdminBypass() throws Exception {

        TestContext context = createContext("legacy-bypass");

        /*
         * Deliberately has the old TENANT_ADMIN role.
         */
        AppUser legacyAdministrator =
                createUser(
                        context.tenant(),
                        "Legacy Administrator With V2 Scope",
                        UserRole.TENANT_ADMIN);

        AuthorizationRoleResponse readOnlyRole =
                createRole(
                        context.tenant(),
                        "READ_ONLY_V2_ROLE",
                        Set.of(PlatformPermissionCodes.TENANT_READ));

        assignRole(context, legacyAdministrator, readOnlyRole, AuthorizationScopeType.TENANT, null);

        String accessToken = login(context.tenant().getId(), legacyAdministrator.getEmail());

        /*
         * The existence of a V2 assignment makes V2
         * authoritative. The old TENANT_ADMIN role cannot
         * restore authorization.manage or audit.read.
         */
        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}" + "/authorization/permissions",
                                        context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/tenants/{tenantId}" + "/audit-logs", context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden());
    }

    private TestContext createContext(String prefix) {
        String suffix = uniqueSuffix();

        TenantOnboardingResponse onboarding =
                tenantOnboardingService.onboardTenant(
                        new TenantOnboardingRequest(
                                prefix + " Tenant",
                                prefix + "-" + suffix,
                                prefix + " Administrator",
                                prefix + ".admin." + suffix + "@example.test",
                                PASSWORD));

        Tenant tenant = tenantRepository.findById(onboarding.tenant().id()).orElseThrow();

        AppUser administrator =
                appUserRepository
                        .findByTenantIdAndId(tenant.getId(), onboarding.adminUser().id())
                        .orElseThrow();

        return new TestContext(tenant, administrator);
    }

    private AuthorizationRoleResponse createRole(
            Tenant tenant, String roleCode, Set<String> permissionCodes) {
        Set<UUID> permissionIds =
                permissionCodes.stream()
                        .map(this::getPermissionId)
                        .collect(java.util.stream.Collectors.toSet());

        return authorizationRoleService.createTenantRole(
                tenant.getId(),
                new AuthorizationRoleCreateRequest(
                        roleCode, roleCode.replace('_', ' '), null, permissionIds));
    }

    private UUID getPermissionId(String code) {
        return permissionRepository
                .findBySourceAndCode(AuthorizationPermissionSource.PLATFORM, code)
                .orElseThrow()
                .getId();
    }

    private void assignRole(
            TestContext context,
            AppUser user,
            AuthorizationRoleResponse role,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId) {
        assignmentService.createAssignment(
                context.tenant().getId(),
                context.administrator().getId(),
                new AuthorizationUserRoleAssignmentCreateRequest(
                        user.getId(),
                        role.id(),
                        scopeType,
                        scopeTargetId,
                        Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS),
                        null));
    }

    private AppUser createUser(Tenant tenant, String fullName, UserRole legacyRole) {
        String suffix = uniqueSuffix();

        AppUser user =
                new AppUser(
                        tenant,
                        fullName,
                        "delegated." + suffix + "@example.test",
                        passwordEncoder.encode(PASSWORD),
                        legacyRole);

        return appUserRepository.saveAndFlush(user);
    }

    private String login(UUID tenantId, String email) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}" + "/auth/login", tenantId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "email": "%s",
                                                  "password": "%s"
                                                }
                                                """
                                                        .formatted(email, PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return response.path("data").path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record TestContext(Tenant tenant, AppUser administrator) {}
}
