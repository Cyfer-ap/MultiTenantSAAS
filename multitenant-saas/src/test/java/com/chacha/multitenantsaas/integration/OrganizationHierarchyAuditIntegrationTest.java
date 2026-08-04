package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationHierarchyAuditIntegrationTest {

    private static final String PASSWORD =
            "OrganizationAuditAdmin@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthorizationProvisioningService
            authorizationProvisioningService;

    @Test
    void hierarchyMutationsCreateTenantAuditEntries()
            throws Exception {

        Tenant tenant = createTenant(
                "organization-audit"
        );

        AppUser admin = createUser(
                tenant,
                "Organization Audit Admin"
        );

        String accessToken = login(
                tenant.getId(),
                admin.getEmail()
        );

        UUID destinationUnitId = createUnit(
                tenant.getId(),
                accessToken,
                null,
                "Operations",
                "OPERATIONS",
                "DEPARTMENT"
        );

        UUID targetUnitId = createUnit(
                tenant.getId(),
                accessToken,
                null,
                "Engineering",
                "ENGINEERING",
                "DIVISION"
        );

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}",
                                tenant.getId(),
                                targetUnitId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "name": "Product Engineering",
                                          "code": "PRODUCT-ENGINEERING",
                                          "type": "DEPARTMENT"
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/status",
                                tenant.getId(),
                                targetUnitId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "status": "INACTIVE"
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/move",
                                tenant.getId(),
                                targetUnitId
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
                                          "parentUnitId": "%s"
                                        }
                                        """.formatted(
                                                destinationUnitId
                                        )
                                )
                )
                .andExpect(status().isOk());

        assertAuditAction(
                tenant.getId(),
                admin,
                accessToken,
                "ORG_UNIT_CREATED",
                2
        );

        assertAuditAction(
                tenant.getId(),
                admin,
                accessToken,
                "ORG_UNIT_UPDATED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                admin,
                accessToken,
                "ORG_UNIT_STATUS_UPDATED",
                1
        );

        assertAuditAction(
                tenant.getId(),
                admin,
                accessToken,
                "ORG_UNIT_MOVED",
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
                                        "ORG_UNIT_MOVED"
                                )
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.content[0].message",
                                containsString(
                                        targetUnitId.toString()
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].message",
                                containsString(
                                        destinationUnitId.toString()
                                )
                        )
                );
    }

    private void assertAuditAction(
            UUID tenantId,
            AppUser expectedActor,
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
                                .param(
                                        "sortBy",
                                        "createdAt"
                                )
                                .param(
                                        "sortDir",
                                        "desc"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.data.totalElements"
                        )
                                .value(expectedCount)
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].tenantId"
                        )
                                .value(
                                        tenantId.toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].actorType"
                        )
                                .value("TENANT_USER")
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].actorUserId"
                        )
                                .value(
                                        expectedActor
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].actorUserEmail"
                        )
                                .value(
                                        expectedActor
                                                .getEmail()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].targetUserId"
                        )
                                .value(
                                        expectedActor
                                                .getId()
                                                .toString()
                                )
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

    private UUID createUnit(
            UUID tenantId,
            String accessToken,
            UUID parentUnitId,
            String name,
            String code,
            String type
    ) throws Exception {
        String parentValue =
                parentUnitId == null
                        ? "null"
                        : "\""
                        + parentUnitId
                        + "\"";

        String requestBody =
                """
                {
                  "parentUnitId": %s,
                  "name": "%s",
                  "code": "%s",
                  "type": "%s"
                }
                """.formatted(
                        parentValue,
                        name,
                        code,
                        type
                );

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}"
                                                + "/organization/units",
                                        tenantId
                                )
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                bearer(accessToken)
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(requestBody)
                        )
                        .andExpect(status().isOk())
                        .andReturn();

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

    private Tenant createTenant(String prefix) {
        String suffix = uniqueSuffix();

        Tenant tenant = new Tenant(
                prefix + " Tenant",
                prefix + "-" + suffix
        );

        return tenantRepository
                .saveAndFlush(tenant);
    }

    private AppUser createUser(
            Tenant tenant,
            String fullName
    ) {
        AppUser admin = new AppUser(
                tenant,
                fullName,
                "organization.audit."
                        + uniqueSuffix()
                        + "@example.test",
                passwordEncoder.encode(PASSWORD),
                UserRole.TENANT_ADMIN
        );

        AppUser savedAdmin =
                appUserRepository.saveAndFlush(admin);

        authorizationProvisioningService
                .synchronizeUserFromLegacyState(
                        tenant.getId(),
                        savedAdmin.getId()
                );

        return savedAdmin;
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