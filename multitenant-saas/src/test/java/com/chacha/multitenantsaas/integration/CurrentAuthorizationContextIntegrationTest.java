package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.chacha.multitenantsaas.service.AuthorizationProvisioningService;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
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
class CurrentAuthorizationContextIntegrationTest {

    private static final String PASSWORD = "AuthorizationContext@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Autowired private TenantOnboardingService tenantOnboardingService;

    @Autowired private AuthorizationProvisioningService authorizationProvisioningService;

    @Autowired private AuthorizationRoleService authorizationRoleService;

    @Autowired private AuthorizationUserRoleAssignmentService assignmentService;

    @Autowired private AuthorizationPermissionRepository permissionRepository;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void currentUserReceivesTenantAndScopedGrants() throws Exception {

        TestContext firstContext = createContext("auth-context-first");

        TestContext secondContext = createContext("auth-context-second");

        AppUser member = createProvisionedMember(firstContext.tenant());

        AuthorizationRoleResponse selfAuditorRole = createSelfAuditorRole(firstContext.tenant());

        assignmentService.createAssignment(
                firstContext.tenant().getId(),
                firstContext.administrator().getId(),
                new AuthorizationUserRoleAssignmentCreateRequest(
                        member.getId(),
                        selfAuditorRole.id(),
                        AuthorizationScopeType.SELF,
                        null,
                        Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS),
                        null));

        String accessToken = login(firstContext.tenant().getId(), member.getEmail());

        MvcResult result =
                mockMvc.perform(
                                get(
                                                "/api/tenants/{tenantId}" + "/authorization/me",
                                                firstContext.tenant().getId())
                                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.userId").value(member.getId().toString()))
                        .andExpect(jsonPath("$.data.grants.length()").value(2))
                        .andReturn();

        JsonNode data = jsonMapper.readTree(result.getResponse().getContentAsString()).path("data");

        Set<String> roleCodes =
                StreamSupport.stream(data.path("grants").spliterator(), false)
                        .map(grant -> grant.path("roleCode").asText())
                        .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("MEMBER", "SELF_AUDITOR"), roleCodes);

        Set<String> tenantPermissions = stringSet(data.path("tenantPermissionCodes"));

        assertTrue(tenantPermissions.contains(PlatformPermissionCodes.TENANT_READ));

        assertTrue(tenantPermissions.contains(PlatformPermissionCodes.PROJECT_READ));

        assertFalse(tenantPermissions.contains(PlatformPermissionCodes.AUDIT_READ));

        Set<String> allPermissions = stringSet(data.path("allPermissionCodes"));

        assertTrue(allPermissions.contains(PlatformPermissionCodes.AUDIT_READ));

        JsonNode selfGrant =
                StreamSupport.stream(data.path("grants").spliterator(), false)
                        .filter(grant -> "SELF_AUDITOR".equals(grant.path("roleCode").asText()))
                        .findFirst()
                        .orElseThrow();

        assertEquals("SELF", selfGrant.path("scopeType").asText());

        assertTrue(
                stringSet(selfGrant.path("permissionCodes"))
                        .contains(PlatformPermissionCodes.AUDIT_READ));

        /*
         * The JWT tenant cannot inspect another tenant's
         * authorization context.
         */
        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}" + "/authorization/me",
                                        secondContext.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unprovisionedLegacyUserReceivesEmptyContext() throws Exception {

        TestContext context = createContext("empty-context");

        AppUser unprovisionedLegacyAdmin = createUnprovisionedLegacyAdmin(context.tenant());

        String accessToken = login(context.tenant().getId(), unprovisionedLegacyAdmin.getEmail());

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}" + "/authorization/me",
                                        context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grants.length()").value(0))
                .andExpect(jsonPath("$.data" + ".tenantPermissionCodes" + ".length()").value(0))
                .andExpect(jsonPath("$.data" + ".allPermissionCodes" + ".length()").value(0));
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

    private AppUser createProvisionedMember(Tenant tenant) {
        AppUser member =
                new AppUser(
                        tenant,
                        "Authorization Context Member",
                        "context.member." + uniqueSuffix() + "@example.test",
                        passwordEncoder.encode(PASSWORD),
                        UserRole.TENANT_USER);

        AppUser savedMember = appUserRepository.saveAndFlush(member);

        authorizationProvisioningService.synchronizeUserFromLegacyState(
                tenant.getId(), savedMember.getId());

        return savedMember;
    }

    private AppUser createUnprovisionedLegacyAdmin(Tenant tenant) {
        AppUser administrator =
                new AppUser(
                        tenant,
                        "Unprovisioned Context Admin",
                        "context.legacy." + uniqueSuffix() + "@example.test",
                        passwordEncoder.encode(PASSWORD),
                        UserRole.TENANT_ADMIN);

        return appUserRepository.saveAndFlush(administrator);
    }

    private AuthorizationRoleResponse createSelfAuditorRole(Tenant tenant) {
        UUID auditReadPermissionId =
                permissionRepository
                        .findBySourceAndCode(
                                AuthorizationPermissionSource.PLATFORM,
                                PlatformPermissionCodes.AUDIT_READ)
                        .orElseThrow()
                        .getId();

        return authorizationRoleService.createTenantRole(
                tenant.getId(),
                new AuthorizationRoleCreateRequest(
                        "SELF_AUDITOR",
                        "Self Auditor",
                        "Reads only the current " + "user's audit context.",
                        Set.of(auditReadPermissionId)));
    }

    private Set<String> stringSet(JsonNode arrayNode) {
        Set<String> result = new HashSet<>();

        arrayNode.forEach(value -> result.add(value.asText()));

        return result;
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
