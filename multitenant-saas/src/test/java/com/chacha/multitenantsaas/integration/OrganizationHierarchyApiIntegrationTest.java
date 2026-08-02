package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

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
@Transactional
class OrganizationHierarchyApiIntegrationTest {

    private static final String PASSWORD =
            "OrganizationAdmin@123";

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

    @Test
    void tenantAdminCanManageOrganizationHierarchy()
            throws Exception {

        Tenant tenant = createTenant(
                "organization-api"
        );

        AppUser admin = createUser(
                tenant,
                "Organization Admin",
                UserRole.TENANT_ADMIN
        );

        String adminToken = login(
                tenant.getId(),
                admin.getEmail()
        );

        UUID companyId = createUnit(
                tenant.getId(),
                adminToken,
                null,
                "Company",
                "company",
                "COMPANY"
        );

        UUID engineeringId = createUnit(
                tenant.getId(),
                adminToken,
                companyId,
                "Engineering",
                "engineering",
                "DIVISION"
        );

        UUID operationsId = createUnit(
                tenant.getId(),
                adminToken,
                null,
                "Operations",
                "operations",
                "DEPARTMENT"
        );

        /*
         * Read roots.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/roots",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.data[0].name")
                                .value("Company")
                )
                .andExpect(
                        jsonPath("$.data[1].name")
                                .value("Operations")
                );

        /*
         * Read the complete nested tree.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/tree",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.data[0].id")
                                .value(companyId.toString())
                )
                .andExpect(
                        jsonPath(
                                "$.data[0].children[0].id"
                        )
                                .value(
                                        engineeringId.toString()
                                )
                );

        /*
         * Read one unit and its direct children.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}",
                                tenant.getId(),
                                engineeringId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.parentUnitId")
                                .value(companyId.toString())
                )
                .andExpect(
                        jsonPath("$.data.code")
                                .value("ENGINEERING")
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/children",
                                tenant.getId(),
                                companyId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.data[0].id")
                                .value(
                                        engineeringId.toString()
                                )
                );

        /*
         * Update details.
         */
        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}",
                                tenant.getId(),
                                engineeringId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "name": "Product Engineering",
                                          "code": "product_eng",
                                          "type": "DEPARTMENT"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.name")
                                .value(
                                        "Product Engineering"
                                )
                )
                .andExpect(
                        jsonPath("$.data.code")
                                .value("PRODUCT_ENG")
                )
                .andExpect(
                        jsonPath("$.data.type")
                                .value("DEPARTMENT")
                );

        /*
         * Move the unit beneath Operations.
         */
        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/move",
                                tenant.getId(),
                                engineeringId
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
                                          "parentUnitId": "%s"
                                        }
                                        """.formatted(
                                                operationsId
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.parentUnitId")
                                .value(
                                        operationsId.toString()
                                )
                );

        /*
         * Verify rebuilt ancestor paths.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/ancestors",
                                tenant.getId(),
                                engineeringId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.data[0].id")
                                .value(
                                        engineeringId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.data[0].depth")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.data[1].id")
                                .value(
                                        operationsId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.data[1].depth")
                                .value(1)
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/descendants",
                                tenant.getId(),
                                operationsId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.length()")
                                .value(2)
                );

        /*
         * Change status.
         */
        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/status",
                                tenant.getId(),
                                engineeringId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
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
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.status")
                                .value("INACTIVE")
                );

        /*
         * Read the destination subtree.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/subtree",
                                tenant.getId(),
                                operationsId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.id")
                                .value(
                                        operationsId.toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.children[0].id"
                        )
                                .value(
                                        engineeringId.toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.children[0].status"
                        )
                                .value("INACTIVE")
                );
    }

    @Test
    void managerAndRegularUserCannotAccessOrganizationApis()
            throws Exception {

        Tenant tenant = createTenant(
                "organization-role"
        );

        AppUser admin = createUser(
                tenant,
                "Tenant Admin",
                UserRole.TENANT_ADMIN
        );

        AppUser manager = createUser(
                tenant,
                "Tenant Manager",
                UserRole.TENANT_MANAGER
        );

        AppUser regularUser = createUser(
                tenant,
                "Tenant User",
                UserRole.TENANT_USER
        );

        String adminToken = login(
                tenant.getId(),
                admin.getEmail()
        );

        String managerToken = login(
                tenant.getId(),
                manager.getEmail()
        );

        String userToken = login(
                tenant.getId(),
                regularUser.getEmail()
        );

        UUID unitId = createUnit(
                tenant.getId(),
                adminToken,
                null,
                "Restricted Unit",
                "RESTRICTED",
                "DIVISION"
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/tree",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(managerToken)
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.errorCode")
                                .value("ACCESS_DENIED")
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}",
                                tenant.getId(),
                                unitId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(userToken)
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.errorCode")
                                .value("ACCESS_DENIED")
                );

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(managerToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "parentUnitId": null,
                                          "name": "Denied Unit",
                                          "code": "DENIED",
                                          "type": "TEAM"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}"
                                        + "/status",
                                tenant.getId(),
                                unitId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(userToken)
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
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationApisRejectCrossTenantAccess()
            throws Exception {

        Tenant firstTenant = createTenant(
                "organization-first"
        );

        Tenant secondTenant = createTenant(
                "organization-second"
        );

        AppUser firstAdmin = createUser(
                firstTenant,
                "First Admin",
                UserRole.TENANT_ADMIN
        );

        AppUser secondAdmin = createUser(
                secondTenant,
                "Second Admin",
                UserRole.TENANT_ADMIN
        );

        String firstToken = login(
                firstTenant.getId(),
                firstAdmin.getEmail()
        );

        String secondToken = login(
                secondTenant.getId(),
                secondAdmin.getEmail()
        );

        UUID firstTenantUnit = createUnit(
                firstTenant.getId(),
                firstToken,
                null,
                "First Tenant Unit",
                "FIRST-UNIT",
                "DIVISION"
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/{unitId}",
                                firstTenant.getId(),
                                firstTenantUnit
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(secondToken)
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.errorCode")
                                .value("ACCESS_DENIED")
                );
    }

    @Test
    void organizationApisValidateRequestsAndAuthentication()
            throws Exception {

        Tenant tenant = createTenant(
                "organization-validation"
        );

        AppUser admin = createUser(
                tenant,
                "Validation Admin",
                UserRole.TENANT_ADMIN
        );

        String adminToken = login(
                tenant.getId(),
                admin.getEmail()
        );

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "parentUnitId": null,
                                          "name": "   ",
                                          "code": "VALID",
                                          "type": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errorCode")
                                .value("VALIDATION_FAILED")
                )
                .andExpect(
                        jsonPath("$.details.name")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.details.type")
                                .exists()
                );

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "parentUnitId": null,
                                          "name": "Invalid Code Unit",
                                          "code": "INVALID CODE",
                                          "type": "TEAM"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errorCode")
                                .value("INVALID_REQUEST")
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/units/tree",
                                tenant.getId()
                        )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.errorCode")
                                .value(
                                        "AUTHENTICATION_REQUIRED"
                                )
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
                        .andExpect(
                                jsonPath("$.success")
                                        .value(true)
                        )
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
        String requestBody =
                """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                        email,
                        PASSWORD
                );

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
                                        .content(requestBody)
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
        String suffix = uniqueSuffix();

        AppUser user = new AppUser(
                tenant,
                fullName,
                role.name()
                        .toLowerCase()
                        .replace('_', '.')
                        + "."
                        + suffix
                        + "@example.test",
                passwordEncoder.encode(PASSWORD),
                role
        );

        return appUserRepository
                .saveAndFlush(user);
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