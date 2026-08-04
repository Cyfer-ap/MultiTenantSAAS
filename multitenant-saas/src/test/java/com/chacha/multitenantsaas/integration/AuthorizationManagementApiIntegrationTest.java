package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermission;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.service.AuthorizationProvisioningService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationManagementApiIntegrationTest {

    private static final String PASSWORD =
            "AuthorizationManagementAdmin@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AuthorizationPermissionRepository
            authorizationPermissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthorizationProvisioningService
            authorizationProvisioningService;

    @Test
    void adminCanManageAuthorizationLifecycleAndAudit()
            throws Exception {

        Tenant tenant =
                createTenant("authorization-api");

        AppUser administrator =
                createUser(
                        tenant,
                        "Authorization API Administrator",
                        UserRole.TENANT_ADMIN
                );

        AppUser member =
                createUser(
                        tenant,
                        "Authorization API Member",
                        UserRole.TENANT_USER
                );

        String accessToken =
                login(
                        tenant.getId(),
                        administrator.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/permissions",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data", hasSize(19))
                );

        UUID customPermissionId =
                createCustomPermission(
                        tenant.getId(),
                        accessToken,
                        "custom.invoice.approve"
                );

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/roles"
                                        + "/defaults/initialize",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data", hasSize(3))
                );

        AuthorizationPermission projectRead =
                getPlatformPermission(
                        PlatformPermissionCodes.PROJECT_READ
                );

        UUID customRoleId =
                createCustomRole(
                        tenant.getId(),
                        accessToken,
                        projectRead.getId(),
                        customPermissionId
                );

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/roles"
                                        + "/{roleId}/permissions",
                                tenant.getId(),
                                customRoleId
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
                                          "permissionIds": [
                                            "%s"
                                          ]
                                        }
                                        """.formatted(
                                                customPermissionId
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.permissions",
                                hasSize(1)
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.permissions[0].code"
                        )
                                .value(
                                        "custom.invoice.approve"
                                )
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

        UUID assignmentId =
                createTenantRoleAssignment(
                        tenant.getId(),
                        accessToken,
                        member.getId(),
                        customRoleId,
                        validFrom
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/assignments"
                                        + "/users/{userId}/effective",
                                tenant.getId(),
                                member.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data", hasSize(2))
                )
                .andExpect(
                        jsonPath(
                                "$.data[*].id",
                                hasItem(
                                        assignmentId.toString()
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data[*].roleCode",
                                containsInAnyOrder(
                                        "MEMBER",
                                        "INVOICE_APPROVER"
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data[*].scopeType",
                                everyItem(is("TENANT"))
                        )
                );

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/assignments"
                                        + "/{assignmentId}/deactivate",
                                tenant.getId(),
                                assignmentId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.status")
                                .value("INACTIVE")
                );

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/roles"
                                        + "/{roleId}/deactivate",
                                tenant.getId(),
                                customRoleId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.status")
                                .value("INACTIVE")
                );

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/permissions"
                                        + "/custom/{permissionId}"
                                        + "/deactivate",
                                tenant.getId(),
                                customPermissionId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.status")
                                .value("INACTIVE")
                );

        assertAuditAction(
                tenant.getId(),
                accessToken,
                "AUTH_PERMISSION_CREATED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                accessToken,
                "AUTH_ROLES_INITIALIZED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                accessToken,
                "AUTH_ROLE_CREATED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                accessToken,
                "AUTH_ROLE_PERMISSIONS_UPDATED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                accessToken,
                "AUTH_USER_ROLE_ASSIGNED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                accessToken,
                "AUTH_USER_ROLE_ASSIGNMENT_DEACTIVATED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                accessToken,
                "AUTH_ROLE_DEACTIVATED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                accessToken,
                "AUTH_PERMISSION_DEACTIVATED",
                1
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}/audit-logs",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .param(
                                        "action",
                                        "AUTH_USER_ROLE_ASSIGNED"
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.content[0].actorUserId"
                        )
                                .value(
                                        administrator
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].targetUserId"
                        )
                                .value(
                                        member.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].message",
                                containsString(
                                        assignmentId.toString()
                                )
                        )
                );
    }

    @Test
    void managementApiValidatesRoleAndPermissionRequests()
            throws Exception {

        Tenant tenant =
                createTenant("authorization-validation");

        AppUser administrator =
                createUser(
                        tenant,
                        "Authorization Validation Admin",
                        UserRole.TENANT_ADMIN
                );

        String accessToken =
                login(
                        tenant.getId(),
                        administrator.getEmail()
                );

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/permissions"
                                        + "/custom",
                                tenant.getId()
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
                                          "code": "project.read",
                                          "name": "Invalid override",
                                          "category": "PROJECT"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/roles",
                                tenant.getId()
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
                                          "code": "ADMIN",
                                          "name": "Invalid Admin",
                                          "permissionIds": []
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/assignments",
                                tenant.getId()
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
                                          "scopeType": "TENANT"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void managementApiEnforcesRoleAndTenantIsolation()
            throws Exception {

        Tenant firstTenant =
                createTenant("authorization-security-first");

        Tenant secondTenant =
                createTenant("authorization-security-second");

        AppUser firstAdministrator =
                createUser(
                        firstTenant,
                        "First Authorization Admin",
                        UserRole.TENANT_ADMIN
                );

        AppUser firstRegularUser =
                createUser(
                        firstTenant,
                        "First Authorization User",
                        UserRole.TENANT_USER
                );

        AppUser secondAdministrator =
                createUser(
                        secondTenant,
                        "Second Authorization Admin",
                        UserRole.TENANT_ADMIN
                );

        String firstAdminToken =
                login(
                        firstTenant.getId(),
                        firstAdministrator.getEmail()
                );

        String firstUserToken =
                login(
                        firstTenant.getId(),
                        firstRegularUser.getEmail()
                );

        String secondAdminToken =
                login(
                        secondTenant.getId(),
                        secondAdministrator.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/permissions",
                                firstTenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(firstUserToken)
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/permissions",
                                secondTenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(firstAdminToken)
                                )
                )
                .andExpect(status().isForbidden());

        UUID secondPermissionId =
                createCustomPermission(
                        secondTenant.getId(),
                        secondAdminToken,
                        "custom.second.export"
                );

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/authorization/roles",
                                firstTenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(firstAdminToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "code": "CROSS_TENANT_ROLE",
                                          "name": "Cross Tenant Role",
                                          "permissionIds": [
                                            "%s"
                                          ]
                                        }
                                        """.formatted(
                                                secondPermissionId
                                        )
                                )
                )
                .andExpect(status().isNotFound());
    }

    private UUID createCustomPermission(
            UUID tenantId,
            String accessToken,
            String code
    ) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}"
                                                + "/authorization"
                                                + "/permissions/custom",
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
                                                  "code": "%s",
                                                  "name": "Approve invoices",
                                                  "description":
                                                    "Approve invoices.",
                                                  "category": "FINANCE"
                                                }
                                                """.formatted(code)
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        return extractDataId(result);
    }

    private UUID createCustomRole(
            UUID tenantId,
            String accessToken,
            UUID firstPermissionId,
            UUID secondPermissionId
    ) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}"
                                                + "/authorization/roles",
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
                                                  "code":
                                                    "INVOICE_APPROVER",
                                                  "name":
                                                    "Invoice Approver",
                                                  "description":
                                                    "Approves invoices.",
                                                  "permissionIds": [
                                                    "%s",
                                                    "%s"
                                                  ]
                                                }
                                                """.formatted(
                                                        firstPermissionId,
                                                        secondPermissionId
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        return extractDataId(result);
    }

    private UUID createTenantRoleAssignment(
            UUID tenantId,
            String accessToken,
            UUID userId,
            UUID roleId,
            Instant validFrom
    ) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}"
                                                + "/authorization"
                                                + "/assignments",
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
                                                  "userId": "%s",
                                                  "roleId": "%s",
                                                  "scopeType": "TENANT",
                                                  "scopeTargetId": null,
                                                  "validFrom": "%s",
                                                  "validUntil": null
                                                }
                                                """.formatted(
                                                        userId,
                                                        roleId,
                                                        validFrom
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        return extractDataId(result);
    }

    private void assertAuditAction(
            UUID tenantId,
            String accessToken,
            String action,
            int expectedCount
    ) throws Exception {
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}/audit-logs",
                                tenantId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .param("action", action)
                                .param("success", "true")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.totalElements"
                        )
                                .value(expectedCount)
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].action"
                        )
                                .value(action)
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].success"
                        )
                                .value(true)
                );
    }

    private AuthorizationPermission
    getPlatformPermission(String code) {
        return authorizationPermissionRepository
                .findBySourceAndCode(
                        AuthorizationPermissionSource.PLATFORM,
                        code
                )
                .orElseThrow();
    }

    private UUID extractDataId(
            MvcResult result
    ) throws Exception {
        JsonNode responseBody =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return UUID.fromString(
                responseBody
                        .path("data")
                        .path("id")
                        .asText()
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

        JsonNode responseBody =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return responseBody
                .path("data")
                .path("accessToken")
                .asText();
    }

    private AppUser createUser(
            Tenant tenant,
            String fullName,
            UserRole role
    ) {
        AppUser user = new AppUser(
                tenant,
                fullName,
                role.name()
                        .toLowerCase()
                        .replace('_', '.')
                        + "."
                        + uniqueSuffix()
                        + "@example.test",
                passwordEncoder.encode(PASSWORD),
                role
        );

        AppUser savedUser =
                appUserRepository.saveAndFlush(user);

        authorizationProvisioningService
                .synchronizeUserFromLegacyState(
                        tenant.getId(),
                        savedUser.getId()
                );

        return savedUser;
    }

    private Tenant createTenant(String prefix) {
        String suffix = uniqueSuffix();

        Tenant tenant = new Tenant(
                prefix + " Tenant",
                prefix + "-" + suffix
        );

        return tenantRepository
                .saveAndFlush(tenant);
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}