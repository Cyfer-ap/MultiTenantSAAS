package com.chacha.multitenantsaas.integration;

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
class AuthorizationExplainAccessIntegrationTest {

    private static final String PASSWORD = "ExplainAccess@123";

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
    void explainsGrantedMissingAndScopeMismatchedAccessFromTheEnforcementEvaluator()
            throws Exception {
        TestContext context = createContext("explain-access");

        AppUser manager = createUser(context.tenant(), "Explain Access Manager");
        AppUser anotherUser = createUser(context.tenant(), "Explain Access Target");

        AuthorizationRoleResponse managementRole =
                createRole(
                        context.tenant(),
                        "EXPLAIN_ACCESS_MANAGER",
                        Set.of(PlatformPermissionCodes.AUTHORIZATION_MANAGE));
        AuthorizationRoleResponse selfReaderRole =
                createRole(
                        context.tenant(),
                        "SELF_USER_READER",
                        Set.of(PlatformPermissionCodes.USER_READ));

        assignRole(context, manager, managementRole, AuthorizationScopeType.TENANT, null);
        assignRole(context, manager, selfReaderRole, AuthorizationScopeType.SELF, null);

        String accessToken = login(context.tenant().getId(), manager.getEmail());

        explain(
                        accessToken,
                        context.tenant().getId(),
                        manager.getId(),
                        PlatformPermissionCodes.AUTHORIZATION_MANAGE,
                        "TENANT",
                        null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.granted").value(true))
                .andExpect(jsonPath("$.data.reason").value("GRANTED_BY_ROLE_ASSIGNMENT"))
                .andExpect(jsonPath("$.data.matchedGrant.roleCode").value("EXPLAIN_ACCESS_MANAGER"))
                .andExpect(jsonPath("$.data.matchedGrant.scopeType").value("TENANT"));

        explain(
                        accessToken,
                        context.tenant().getId(),
                        manager.getId(),
                        PlatformPermissionCodes.USER_READ,
                        "USER",
                        manager.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.granted").value(true))
                .andExpect(jsonPath("$.data.reason").value("GRANTED_BY_ROLE_ASSIGNMENT"))
                .andExpect(jsonPath("$.data.matchedGrant.roleCode").value("SELF_USER_READER"))
                .andExpect(jsonPath("$.data.matchedGrant.scopeType").value("SELF"));

        explain(
                        accessToken,
                        context.tenant().getId(),
                        manager.getId(),
                        PlatformPermissionCodes.USER_READ,
                        "USER",
                        anotherUser.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.granted").value(false))
                .andExpect(jsonPath("$.data.reason").value("SCOPE_NOT_SATISFIED"))
                .andExpect(jsonPath("$.data.matchedGrant").isEmpty());

        explain(
                        accessToken,
                        context.tenant().getId(),
                        manager.getId(),
                        PlatformPermissionCodes.AUDIT_READ,
                        "TENANT",
                        null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.granted").value(false))
                .andExpect(jsonPath("$.data.reason").value("NO_EFFECTIVE_GRANT"))
                .andExpect(jsonPath("$.data.matchedGrant").isEmpty());
    }

    @Test
    void rejectsCrossTenantSubjectsAndRequiresAuthorizationManagement() throws Exception {
        TestContext context = createContext("explain-boundary");
        TestContext anotherTenant = createContext("explain-boundary-other");

        AppUser manager = createUser(context.tenant(), "Boundary Explain Manager");
        AuthorizationRoleResponse managementRole =
                createRole(
                        context.tenant(),
                        "BOUNDARY_EXPLAIN_MANAGER",
                        Set.of(PlatformPermissionCodes.AUTHORIZATION_MANAGE));
        assignRole(context, manager, managementRole, AuthorizationScopeType.TENANT, null);

        String managerToken = login(context.tenant().getId(), manager.getEmail());

        explain(
                        managerToken,
                        context.tenant().getId(),
                        anotherTenant.administrator().getId(),
                        PlatformPermissionCodes.TENANT_READ,
                        "TENANT",
                        null)
                .andExpect(status().isNotFound());

        AppUser ordinaryUser = createUser(context.tenant(), "No Explain Permission");
        String ordinaryToken = login(context.tenant().getId(), ordinaryUser.getEmail());

        explain(
                        ordinaryToken,
                        context.tenant().getId(),
                        ordinaryUser.getId(),
                        PlatformPermissionCodes.TENANT_READ,
                        "TENANT",
                        null)
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions explain(
            String accessToken,
            UUID tenantId,
            UUID subjectUserId,
            String permissionCode,
            String contextType,
            UUID targetId)
            throws Exception {
        String targetJson = targetId == null ? "null" : "\"" + targetId + "\"";

        return mockMvc.perform(
                post("/api/tenants/{tenantId}/authorization/explain-access", tenantId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "userId": "%s",
                                  "permissionCode": "%s",
                                  "contextType": "%s",
                                  "targetId": %s
                                }
                                """
                                        .formatted(
                                                subjectUserId,
                                                permissionCode,
                                                contextType,
                                                targetJson)));
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

    private AppUser createUser(Tenant tenant, String fullName) {
        String suffix = uniqueSuffix();
        AppUser user =
                new AppUser(
                        tenant,
                        fullName,
                        "explain." + suffix + "@example.test",
                        passwordEncoder.encode(PASSWORD),
                        UserRole.TENANT_USER);
        return appUserRepository.saveAndFlush(user);
    }

    private String login(UUID tenantId, String email) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/auth/login", tenantId)
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
