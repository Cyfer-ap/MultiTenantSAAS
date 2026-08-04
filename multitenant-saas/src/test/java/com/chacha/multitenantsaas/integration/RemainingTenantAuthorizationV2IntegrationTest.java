package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
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
import com.chacha.multitenantsaas.security.SystemRoleCodes;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class RemainingTenantAuthorizationV2IntegrationTest {

    private static final String PASSWORD =
            "RemainingTenant@123";

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
            assignmentService;

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
    void tenantReadAndUpdatePermissionsAreSeparated()
            throws Exception {

        TestContext context =
                createContext("tenant-profile");

        AppUser reader =
                createUser(
                        context.tenant(),
                        "Delegated Tenant Reader"
                );

        AuthorizationRoleResponse readerRole =
                createRole(
                        context.tenant(),
                        "TENANT_PROFILE_READER",
                        Set.of(
                                PlatformPermissionCodes
                                        .TENANT_READ
                        )
                );

        assignRole(
                context,
                reader,
                readerRole
        );

        String readerToken =
                login(
                        context.tenant().getId(),
                        reader.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(readerToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/tenants/slug/{slug}",
                                context.tenant().getSlug()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(readerToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(readerToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name":
                                            "Unauthorized Tenant Update",
                                          "slug":
                                            "%s"
                                        }
                                        """.formatted(
                                                context.tenant()
                                                        .getSlug()
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        AppUser editor =
                createUser(
                        context.tenant(),
                        "Delegated Tenant Editor"
                );

        AuthorizationRoleResponse editorRole =
                createRole(
                        context.tenant(),
                        "TENANT_PROFILE_EDITOR",
                        Set.of(
                                PlatformPermissionCodes
                                        .TENANT_READ,
                                PlatformPermissionCodes
                                        .TENANT_UPDATE
                        )
                );

        assignRole(
                context,
                editor,
                editorRole
        );

        String editorToken =
                login(
                        context.tenant().getId(),
                        editor.getEmail()
                );

        String updatedSlug =
                "updated-" + uniqueSuffix();

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(editorToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name":
                                            "Updated Tenant Profile",
                                          "slug": "%s"
                                        }
                                        """.formatted(
                                                updatedSlug
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.name")
                                .value(
                                        "Updated Tenant Profile"
                                )
                )
                .andExpect(
                        jsonPath("$.data.slug")
                                .value(updatedSlug)
                );
    }

    @Test
    void invitationAcceptanceCreatesGeneratedV2Role()
            throws Exception {

        TestContext context =
                createContext("invitation-v2");

        AppUser invitationManager =
                createUser(
                        context.tenant(),
                        "Delegated Invitation Manager"
                );

        AuthorizationRoleResponse invitationRole =
                createRole(
                        context.tenant(),
                        "INVITATION_MANAGER",
                        Set.of(
                                PlatformPermissionCodes.USER_READ,
                                PlatformPermissionCodes.USER_CREATE
                        )
                );

        assignRole(
                context,
                invitationManager,
                invitationRole
        );

        String managerToken =
                login(
                        context.tenant().getId(),
                        invitationManager.getEmail()
                );

        String invitedEmail =
                "invited."
                        + uniqueSuffix()
                        + "@example.test";

        MvcResult invitationResult =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}"
                                                + "/user-invitations",
                                        context.tenant().getId()
                                )
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                bearer(managerToken)
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "fullName":
                                                    "Invited Manager",
                                                  "email": "%s",
                                                  "role":
                                                    "TENANT_MANAGER"
                                                }
                                                """.formatted(
                                                        invitedEmail
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.role")
                                        .value(
                                                "TENANT_MANAGER"
                                        )
                        )
                        .andReturn();

        JsonNode invitationBody =
                jsonMapper.readTree(
                        invitationResult
                                .getResponse()
                                .getContentAsString()
                );

        String invitationToken =
                invitationBody
                        .path("data")
                        .path("devInvitationToken")
                        .asText();

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/user-invitations",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(managerToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post(
                                "/api/user-invitations/accept"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "invitationToken": "%s",
                                          "newPassword": "%s",
                                          "confirmPassword": "%s"
                                        }
                                        """.formatted(
                                                invitationToken,
                                                PASSWORD,
                                                PASSWORD
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.user.email")
                                .value(invitedEmail)
                );

        AppUser invitedUser =
                appUserRepository
                        .findByTenantIdAndEmail(
                                context.tenant().getId(),
                                invitedEmail
                        )
                        .orElseThrow();

        List<AuthorizationUserRoleAssignmentResponse>
                effectiveAssignments =
                assignmentService
                        .getEffectiveUserAssignments(
                                context.tenant().getId(),
                                invitedUser.getId(),
                                Instant.now()
                        );

        List<String> systemRoleCodes =
                effectiveAssignments
                        .stream()
                        .map(
                                AuthorizationUserRoleAssignmentResponse
                                        ::roleCode
                        )
                        .filter(
                                SystemRoleCodes.ALL::contains
                        )
                        .toList();

        assertEquals(
                List.of(SystemRoleCodes.MANAGER),
                systemRoleCodes
        );
    }

    @Test
    void dashboardRequiresCompleteAggregatePermissionSet()
            throws Exception {

        TestContext context =
                createContext("dashboard-v2");

        AppUser dashboardReader =
                createUser(
                        context.tenant(),
                        "Dashboard Reader"
                );

        AuthorizationRoleResponse dashboardRole =
                createRole(
                        context.tenant(),
                        "TENANT_DASHBOARD_READER",
                        Set.of(
                                PlatformPermissionCodes.TENANT_READ,
                                PlatformPermissionCodes.USER_READ,
                                PlatformPermissionCodes.PROJECT_READ,
                                PlatformPermissionCodes
                                        .PROJECT_TASK_READ
                        )
                );

        assignRole(
                context,
                dashboardReader,
                dashboardRole
        );

        String dashboardToken =
                login(
                        context.tenant().getId(),
                        dashboardReader.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenant/dashboard/summary"
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(dashboardToken)
                                )
                )
                .andExpect(status().isOk());

        AppUser incompleteReader =
                createUser(
                        context.tenant(),
                        "Incomplete Dashboard Reader"
                );

        AuthorizationRoleResponse incompleteRole =
                createRole(
                        context.tenant(),
                        "INCOMPLETE_DASHBOARD_READER",
                        Set.of(
                                PlatformPermissionCodes.TENANT_READ,
                                PlatformPermissionCodes.USER_READ,
                                PlatformPermissionCodes.PROJECT_READ
                        )
                );

        assignRole(
                context,
                incompleteReader,
                incompleteRole
        );

        String incompleteToken =
                login(
                        context.tenant().getId(),
                        incompleteReader.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenant/dashboard/summary"
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(incompleteToken)
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

        return new TestContext(
                tenant,
                administrator
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
                        .collect(Collectors.toSet());

        return authorizationRoleService
                .createTenantRole(
                        tenant.getId(),
                        new AuthorizationRoleCreateRequest(
                                roleCode,
                                roleCode
                                        .replace('_', ' '),
                                null,
                                permissionIds
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

    private void assignRole(
            TestContext context,
            AppUser user,
            AuthorizationRoleResponse role
    ) {
        assignmentService.createAssignment(
                context.tenant().getId(),
                context.administrator().getId(),
                new AuthorizationUserRoleAssignmentCreateRequest(
                        user.getId(),
                        role.id(),
                        AuthorizationScopeType.TENANT,
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
    }

    private AppUser createUser(
            Tenant tenant,
            String fullName
    ) {
        String suffix = uniqueSuffix();

        AppUser user = new AppUser(
                tenant,
                fullName,
                "remaining."
                        + suffix
                        + "@example.test",
                passwordEncoder.encode(PASSWORD),
                UserRole.TENANT_USER
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
            AppUser administrator
    ) {
    }
}