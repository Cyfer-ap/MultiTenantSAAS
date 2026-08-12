package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationRoleSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
class AuthorizationProvisioningReadinessIntegrationTest {

    private static final String PASSWORD = "ProvisioningReady@123";

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
    void backfillRepairsTenantAndIsIdempotent() throws Exception {

        TestContext context = createContext("readiness");

        AppUser manager =
                createLegacyUser(
                        context.tenant(),
                        "Legacy Manager",
                        UserRole.TENANT_MANAGER,
                        UserStatus.ACTIVE);

        AppUser member =
                createLegacyUser(
                        context.tenant(), "Legacy Member", UserRole.TENANT_USER, UserStatus.ACTIVE);

        createLegacyUser(
                context.tenant(),
                "Inactive Legacy User",
                UserRole.TENANT_USER,
                UserStatus.INACTIVE);

        String administratorToken =
                login(context.tenant().getId(), context.administrator().getEmail());

        mockMvc.perform(
                        get(readinessUrl(), context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(administratorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(false))
                .andExpect(jsonPath("$.data.missingSystemRoleCodes").isEmpty())
                .andExpect(jsonPath("$.data.usersScanned").value(4))
                .andExpect(jsonPath("$.data.activeUsers").value(3))
                .andExpect(jsonPath("$.data.inactiveUsers").value(1))
                .andExpect(jsonPath("$.data.unresolvedUsers").value(2));

        mockMvc.perform(
                        post(backfillUrl(), context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(administratorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provisioning" + ".assignmentsCreated").value(2))
                .andExpect(jsonPath("$.data.provisioning" + ".inactiveUsersSkipped").value(1))
                .andExpect(jsonPath("$.data.readiness.ready").value(true))
                .andExpect(jsonPath("$.data.readiness" + ".unresolvedUsers").value(0));

        assertSingleSystemRole(context.tenant().getId(), manager.getId(), SystemRoleCodes.MANAGER);

        assertSingleSystemRole(context.tenant().getId(), member.getId(), SystemRoleCodes.MEMBER);

        /*
         * Running the operation again must not create
         * duplicate generated assignments.
         */
        mockMvc.perform(
                        post(backfillUrl(), context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(administratorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provisioning" + ".assignmentsCreated").value(0))
                .andExpect(jsonPath("$.data.provisioning" + ".assignmentsAlreadyPresent").value(3))
                .andExpect(jsonPath("$.data.readiness.ready").value(true));
    }

    @Test
    void delegatedAuthorizationManagerCanRunBackfill() throws Exception {

        TestContext context = createContext("delegated-backfill");

        AppUser delegatedManager =
                createLegacyUser(
                        context.tenant(),
                        "Delegated Authorization Manager",
                        UserRole.TENANT_USER,
                        UserStatus.ACTIVE);

        AppUser readOnlyUser =
                createLegacyUser(
                        context.tenant(),
                        "Read Only User",
                        UserRole.TENANT_USER,
                        UserStatus.ACTIVE);

        AuthorizationRoleResponse managerRole =
                createRole(
                        context.tenant(),
                        "PROVISIONING_MANAGER",
                        Set.of(PlatformPermissionCodes.AUTHORIZATION_MANAGE));

        AuthorizationRoleResponse readOnlyRole =
                createRole(
                        context.tenant(),
                        "PROVISIONING_READER",
                        Set.of(PlatformPermissionCodes.TENANT_READ));

        assignRole(context, delegatedManager, managerRole);

        assignRole(context, readOnlyUser, readOnlyRole);

        String delegatedToken = login(context.tenant().getId(), delegatedManager.getEmail());

        String readOnlyToken = login(context.tenant().getId(), readOnlyUser.getEmail());

        mockMvc.perform(
                        get(readinessUrl(), context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(delegatedToken)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post(backfillUrl(), context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(delegatedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readiness.ready").value(true));

        mockMvc.perform(
                        get(readinessUrl(), context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(readOnlyToken)))
                .andExpect(status().isForbidden());
    }

    private void assertSingleSystemRole(UUID tenantId, UUID userId, String expectedRoleCode) {
        List<String> activeSystemRoleCodes =
                assignmentService
                        .getEffectiveUserAssignments(tenantId, userId, Instant.now())
                        .stream()
                        .filter(
                                assignment ->
                                        assignment.roleSource() == AuthorizationRoleSource.SYSTEM)
                        .filter(
                                assignment ->
                                        assignment.scopeType() == AuthorizationScopeType.TENANT)
                        .map(AuthorizationUserRoleAssignmentResponse::roleCode)
                        .sorted()
                        .toList();

        assertEquals(List.of(expectedRoleCode), activeSystemRoleCodes);
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
                permissionCodes.stream().map(this::getPermissionId).collect(Collectors.toSet());

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

    private void assignRole(TestContext context, AppUser user, AuthorizationRoleResponse role) {
        assignmentService.createAssignment(
                context.tenant().getId(),
                context.administrator().getId(),
                new AuthorizationUserRoleAssignmentCreateRequest(
                        user.getId(),
                        role.id(),
                        AuthorizationScopeType.TENANT,
                        null,
                        Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS),
                        null));
    }

    private AppUser createLegacyUser(
            Tenant tenant, String fullName, UserRole role, UserStatus status) {
        String suffix = uniqueSuffix();

        AppUser user =
                new AppUser(
                        tenant,
                        fullName,
                        "provisioning." + suffix + "@example.test",
                        passwordEncoder.encode(PASSWORD),
                        role);

        user.setStatus(status);

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

    private String readinessUrl() {
        return "/api/tenants/{tenantId}" + "/authorization/provisioning" + "/readiness";
    }

    private String backfillUrl() {
        return "/api/tenants/{tenantId}" + "/authorization/provisioning" + "/backfill";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record TestContext(Tenant tenant, AppUser administrator) {}
}
