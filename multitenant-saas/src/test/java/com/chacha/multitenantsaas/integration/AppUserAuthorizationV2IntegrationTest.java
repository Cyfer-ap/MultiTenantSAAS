package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.security.SystemRoleCodes;
import com.chacha.multitenantsaas.service.AuthorizationPermissionEvaluator;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppUserAuthorizationV2IntegrationTest {

    private static final String PASSWORD =
            "AuthorizationUser@123";

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
            authorizationUserRoleAssignmentService;

    @Autowired
    private AuthorizationPermissionEvaluator
            authorizationPermissionEvaluator;

    @Autowired
    private AuthorizationPermissionRepository
            authorizationPermissionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void userLifecycleSynchronizesGeneratedSystemRole()
            throws Exception {

        TenantOnboardingResponse onboarding =
                onboardTenant(
                        "user-sync"
                );

        UUID tenantId =
                onboarding.tenant().id();

        UUID administratorId =
                onboarding.adminUser().id();

        AppUser administrator =
                appUserRepository
                        .findByTenantIdAndId(
                                tenantId,
                                administratorId
                        )
                        .orElseThrow();

        String adminToken =
                login(
                        tenantId,
                        administrator.getEmail()
                );

        UUID userId =
                createUser(
                        tenantId,
                        adminToken,
                        "Synchronized Member",
                        "sync.member."
                                + uniqueSuffix()
                                + "@example.test",
                        UserRole.TENANT_USER
                );

        assertEffectiveSystemRole(
                tenantId,
                userId,
                SystemRoleCodes.MEMBER
        );

        assertTrue(
                hasTenantPermission(
                        tenantId,
                        userId,
                        PlatformPermissionCodes.PROJECT_READ
                )
        );

        assertFalse(
                hasTenantPermission(
                        tenantId,
                        userId,
                        PlatformPermissionCodes.PROJECT_CREATE
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/users/{userId}/role",
                                tenantId,
                                userId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "role": "TENANT_MANAGER"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.role")
                                .value("TENANT_MANAGER")
                );

        assertEffectiveSystemRole(
                tenantId,
                userId,
                SystemRoleCodes.MANAGER
        );

        assertTrue(
                hasTenantPermission(
                        tenantId,
                        userId,
                        PlatformPermissionCodes.PROJECT_CREATE
                )
        );

        assertFalse(
                hasTenantPermission(
                        tenantId,
                        userId,
                        PlatformPermissionCodes
                                .AUTHORIZATION_MANAGE
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/users/{userId}/status",
                                tenantId,
                                userId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "status": "INACTIVE"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.status")
                                .value("INACTIVE")
                );

        assertTrue(
                authorizationUserRoleAssignmentService
                        .getEffectiveUserAssignments(
                                tenantId,
                                userId,
                                Instant.now()
                        )
                        .isEmpty()
        );

        assertFalse(
                hasTenantPermission(
                        tenantId,
                        userId,
                        PlatformPermissionCodes.PROJECT_READ
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/users/{userId}/status",
                                tenantId,
                                userId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "status": "ACTIVE"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.status")
                                .value("ACTIVE")
                );

        assertEffectiveSystemRole(
                tenantId,
                userId,
                SystemRoleCodes.MANAGER
        );

        assertTrue(
                hasTenantPermission(
                        tenantId,
                        userId,
                        PlatformPermissionCodes.PROJECT_CREATE
                )
        );
    }

    @Test
    void scopedSelfPermissionProtectsUserEndpoints()
            throws Exception {

        TenantOnboardingResponse onboarding =
                onboardTenant(
                        "user-self-scope"
                );

        UUID tenantId =
                onboarding.tenant().id();

        AppUser administrator =
                appUserRepository
                        .findByTenantIdAndId(
                                tenantId,
                                onboarding.adminUser().id()
                        )
                        .orElseThrow();

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow();

        AppUser scopedUser =
                createUserDirectly(
                        tenant,
                        "Scoped Self User",
                        "scoped.self."
                                + uniqueSuffix()
                                + "@example.test"
                );

        AppUser anotherUser =
                createUserDirectly(
                        tenant,
                        "Another Scoped User",
                        "scoped.other."
                                + uniqueSuffix()
                                + "@example.test"
                );

        AuthorizationPermission userRead =
                authorizationPermissionRepository
                        .findBySourceAndCode(
                                AuthorizationPermissionSource.PLATFORM,
                                PlatformPermissionCodes.USER_READ
                        )
                        .orElseThrow();

        AuthorizationRoleResponse selfViewerRole =
                authorizationRoleService
                        .createTenantRole(
                                tenantId,
                                new AuthorizationRoleCreateRequest(
                                        "SELF_VIEWER",
                                        "Self Viewer",
                                        "Can view only their own "
                                                + "user record.",
                                        Set.of(
                                                userRead.getId()
                                        )
                                )
                        );

        authorizationUserRoleAssignmentService
                .createAssignment(
                        tenantId,
                        administrator.getId(),
                        new AuthorizationUserRoleAssignmentCreateRequest(
                                scopedUser.getId(),
                                selfViewerRole.id(),
                                AuthorizationScopeType.SELF,
                                null,
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

        String scopedUserToken =
                login(
                        tenantId,
                        scopedUser.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/users/{userId}",
                                tenantId,
                                scopedUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(scopedUserToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.id")
                                .value(
                                        scopedUser
                                                .getId()
                                                .toString()
                                )
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/users/{userId}",
                                tenantId,
                                anotherUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(scopedUserToken)
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}/users",
                                tenantId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(scopedUserToken)
                                )
                )
                .andExpect(status().isForbidden());

        String adminToken =
                login(
                        tenantId,
                        administrator.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}/users",
                                tenantId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk());
    }

    private void assertEffectiveSystemRole(
            UUID tenantId,
            UUID userId,
            String expectedRoleCode
    ) {
        List<AuthorizationUserRoleAssignmentResponse>
                effectiveAssignments =
                authorizationUserRoleAssignmentService
                        .getEffectiveUserAssignments(
                                tenantId,
                                userId,
                                Instant.now()
                        );

        List<AuthorizationUserRoleAssignmentResponse>
                generatedSystemAssignments =
                effectiveAssignments
                        .stream()
                        .filter(
                                assignment ->
                                        assignment.scopeType()
                                                == AuthorizationScopeType.TENANT
                        )
                        .filter(
                                assignment ->
                                        SystemRoleCodes.ALL
                                                .contains(
                                                        assignment
                                                                .roleCode()
                                                )
                        )
                        .toList();

        assertEquals(
                1,
                generatedSystemAssignments.size()
        );

        assertEquals(
                expectedRoleCode,
                generatedSystemAssignments
                        .getFirst()
                        .roleCode()
        );
    }

    private boolean hasTenantPermission(
            UUID tenantId,
            UUID userId,
            String permissionCode
    ) {
        return authorizationPermissionEvaluator
                .hasPermission(
                        tenantId,
                        userId,
                        permissionCode,
                        AuthorizationEvaluationContext
                                .tenant()
                );
    }

    private UUID createUser(
            UUID tenantId,
            String accessToken,
            String fullName,
            String email,
            UserRole role
    ) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}/users",
                                        tenantId
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
                                                  "fullName": "%s",
                                                  "email": "%s",
                                                  "password": "%s",
                                                  "role": "%s"
                                                }
                                                """.formatted(
                                                        fullName,
                                                        email,
                                                        PASSWORD,
                                                        role
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        return UUID.fromString(
                jsonMapper.readTree(
                                result.getResponse()
                                        .getContentAsString()
                        )
                        .path("data")
                        .path("id")
                        .asText()
        );
    }

    private AppUser createUserDirectly(
            Tenant tenant,
            String fullName,
            String email
    ) {
        AppUser user = new AppUser(
                tenant,
                fullName,
                email,
                passwordEncoder.encode(PASSWORD),
                UserRole.TENANT_USER
        );

        return appUserRepository
                .saveAndFlush(user);
    }

    private TenantOnboardingResponse onboardTenant(
            String prefix
    ) {
        String suffix = uniqueSuffix();

        return tenantOnboardingService
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

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}