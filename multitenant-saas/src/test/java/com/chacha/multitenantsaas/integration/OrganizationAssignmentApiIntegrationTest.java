package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.OrganizationHierarchyService;
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
        .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationAssignmentApiIntegrationTest {

    private static final String PASSWORD =
            "OrganizationAssignmentAdmin@123";

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
    private OrganizationHierarchyService
            organizationHierarchyService;

    @Autowired
    private AuthorizationProvisioningService
            authorizationProvisioningService;

    @Test
    void assignmentEndpointsCreateReadDeactivateAndAudit()
            throws Exception {

        Tenant tenant =
                createTenant("assignment-api");

        AppUser administrator =
                createUser(
                        tenant,
                        "Assignment API Administrator",
                        UserRole.TENANT_ADMIN
                );

        AppUser manager =
                createUser(
                        tenant,
                        "Assignment API Manager",
                        UserRole.TENANT_MANAGER
                );

        AppUser member =
                createUser(
                        tenant,
                        "Assignment API Member",
                        UserRole.TENANT_USER
                );

        OrganizationalUnit engineering =
                createUnit(
                        tenant,
                        null,
                        "Engineering",
                        "ENGINEERING",
                        OrganizationalUnitType.DIVISION
                );

        OrganizationalUnit backend =
                createUnit(
                        tenant,
                        engineering.getId(),
                        "Backend",
                        "BACKEND",
                        OrganizationalUnitType.TEAM
                );

        String accessToken =
                login(
                        tenant.getId(),
                        administrator.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/units/{unitId}"
                                        + "/user-options",
                                tenant.getId(),
                                backend.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data", hasSize(3))
                )
                .andExpect(
                        jsonPath("$.data[0].fullName")
                                .value(
                                        "Assignment API Administrator"
                                )
                )
                .andExpect(
                        jsonPath("$.data[1].fullName")
                                .value(
                                        "Assignment API Manager"
                                )
                )
                .andExpect(
                        jsonPath("$.data[2].fullName")
                                .value(
                                        "Assignment API Member"
                                )
                );

        Instant validFrom =
                Instant.now()
                        .minus(
                                1,
                                ChronoUnit.DAYS
                        );

        UUID managerAssignmentId =
                createAssignment(
                        tenant.getId(),
                        accessToken,
                        manager.getId(),
                        engineering.getId(),
                        null,
                        "Engineering Manager",
                        true,
                        validFrom,
                        null
                );

        UUID memberAssignmentId =
                createAssignment(
                        tenant.getId(),
                        accessToken,
                        member.getId(),
                        backend.getId(),
                        managerAssignmentId,
                        "Backend Engineer",
                        true,
                        validFrom,
                        null
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/{assignmentId}",
                                tenant.getId(),
                                memberAssignmentId
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
                        jsonPath("$.data.id")
                                .value(
                                        memberAssignmentId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.data.userId")
                                .value(
                                        member.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data."
                                        + "organizationalUnitId"
                        )
                                .value(
                                        backend.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data."
                                        + "reportsToAssignmentId"
                        )
                                .value(
                                        managerAssignmentId
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.data.managerUserId")
                                .value(
                                        manager.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.primaryAssignment"
                        )
                                .value(true)
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/users/{userId}",
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
                        jsonPath("$.data", hasSize(1))
                )
                .andExpect(
                        jsonPath("$.data[0].id")
                                .value(
                                        memberAssignmentId
                                                .toString()
                                )
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/users/{userId}/effective",
                                tenant.getId(),
                                member.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .param(
                                        "effectiveAt",
                                        Instant.now().toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data", hasSize(1))
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/units/{unitId}",
                                tenant.getId(),
                                backend.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data", hasSize(1))
                )
                .andExpect(
                        jsonPath("$.data[0].userId")
                                .value(
                                        member.getId()
                                                .toString()
                                )
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/{assignmentId}/direct-reports",
                                tenant.getId(),
                                managerAssignmentId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data", hasSize(1))
                )
                .andExpect(
                        jsonPath("$.data[0].userId")
                                .value(
                                        member.getId()
                                                .toString()
                                )
                );

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/{assignmentId}/deactivate",
                                tenant.getId(),
                                memberAssignmentId
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
                )
                .andExpect(
                        jsonPath("$.data.validUntil")
                                .isNotEmpty()
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
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
                        jsonPath("$.data", hasSize(0))
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/audit-logs",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .param(
                                        "action",
                                        "ORG_ASSIGNMENT_CREATED"
                                )
                                .param("success", "true")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "createdAt")
                                .param("sortDir", "desc")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.totalElements"
                        )
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0]"
                                        + ".actorUserId"
                        )
                                .value(
                                        administrator
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0]"
                                        + ".targetUserId"
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
                                        memberAssignmentId
                                                .toString()
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/audit-logs",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .param(
                                        "action",
                                        "ORG_ASSIGNMENT_DEACTIVATED"
                                )
                                .param("success", "true")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.totalElements"
                        )
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0]"
                                        + ".targetUserId"
                        )
                                .value(
                                        member.getId()
                                                .toString()
                                )
                );
    }

    @Test
    void assignmentApiValidatesRequestsAndDuplicatePrimary()
            throws Exception {

        Tenant tenant =
                createTenant(
                        "assignment-validation"
                );

        AppUser administrator =
                createUser(
                        tenant,
                        "Assignment Validation Admin",
                        UserRole.TENANT_ADMIN
                );

        AppUser member =
                createUser(
                        tenant,
                        "Assignment Validation Member",
                        UserRole.TENANT_USER
                );

        OrganizationalUnit firstUnit =
                createUnit(
                        tenant,
                        null,
                        "First Validation Unit",
                        "FIRST-VALIDATION-UNIT",
                        OrganizationalUnitType.DEPARTMENT
                );

        OrganizationalUnit secondUnit =
                createUnit(
                        tenant,
                        null,
                        "Second Validation Unit",
                        "SECOND-VALIDATION-UNIT",
                        OrganizationalUnitType.DEPARTMENT
                );

        String accessToken =
                login(
                        tenant.getId(),
                        administrator.getEmail()
                );

        Instant start =
                Instant.now()
                        .minus(
                                1,
                                ChronoUnit.DAYS
                        );

        createAssignment(
                tenant.getId(),
                accessToken,
                member.getId(),
                firstUnit.getId(),
                null,
                "Primary Position",
                true,
                start,
                null
        );

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments",
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
                                        assignmentRequestBody(
                                                member.getId(),
                                                secondUnit.getId(),
                                                null,
                                                "Duplicate Primary",
                                                true,
                                                Instant.now(),
                                                null
                                        )
                                )
                )
                .andExpect(status().isConflict());

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments",
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
                                          "positionTitle":
                                            "Missing References",
                                          "primaryAssignment": false
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignmentApiEnforcesRoleAndTenantIsolation()
            throws Exception {

        Tenant firstTenant =
                createTenant("assignment-security-first");

        Tenant secondTenant =
                createTenant("assignment-security-second");

        AppUser firstAdministrator =
                createUser(
                        firstTenant,
                        "First Assignment Admin",
                        UserRole.TENANT_ADMIN
                );

        AppUser firstRegularUser =
                createUser(
                        firstTenant,
                        "First Regular User",
                        UserRole.TENANT_USER
                );

        AppUser secondMember =
                createUser(
                        secondTenant,
                        "Second Tenant Member",
                        UserRole.TENANT_USER
                );

        String firstAdminToken =
                login(
                        firstTenant.getId(),
                        firstAdministrator.getEmail()
                );

        String regularUserToken =
                login(
                        firstTenant.getId(),
                        firstRegularUser.getEmail()
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/users/{userId}",
                                firstTenant.getId(),
                                firstRegularUser.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(regularUserToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data", hasSize(0))
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/organization/assignments"
                                        + "/users/{userId}",
                                secondTenant.getId(),
                                secondMember.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(firstAdminToken)
                                )
                )
                .andExpect(status().isForbidden());
    }

    private UUID createAssignment(
            UUID tenantId,
            String accessToken,
            UUID userId,
            UUID organizationalUnitId,
            UUID reportsToAssignmentId,
            String positionTitle,
            boolean primaryAssignment,
            Instant validFrom,
            Instant validUntil
    ) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}"
                                                + "/organization/assignments",
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
                                                assignmentRequestBody(
                                                        userId,
                                                        organizationalUnitId,
                                                        reportsToAssignmentId,
                                                        positionTitle,
                                                        primaryAssignment,
                                                        validFrom,
                                                        validUntil
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

        return UUID.fromString(
                responseBody
                        .path("data")
                        .path("id")
                        .asText()
        );
    }

    private String assignmentRequestBody(
            UUID userId,
            UUID organizationalUnitId,
            UUID reportsToAssignmentId,
            String positionTitle,
            boolean primaryAssignment,
            Instant validFrom,
            Instant validUntil
    ) {
        String reportsToValue =
                reportsToAssignmentId == null
                        ? "null"
                        : "\""
                        + reportsToAssignmentId
                        + "\"";

        String validUntilValue =
                validUntil == null
                        ? "null"
                        : "\""
                        + validUntil
                        + "\"";

        return """
                {
                  "userId": "%s",
                  "organizationalUnitId": "%s",
                  "reportsToAssignmentId": %s,
                  "positionTitle": "%s",
                  "primaryAssignment": %s,
                  "validFrom": "%s",
                  "validUntil": %s
                }
                """.formatted(
                userId,
                organizationalUnitId,
                reportsToValue,
                positionTitle,
                primaryAssignment,
                validFrom,
                validUntilValue
        );
    }

    private OrganizationalUnit createUnit(
            Tenant tenant,
            UUID parentUnitId,
            String name,
            String code,
            OrganizationalUnitType type
    ) {
        return organizationHierarchyService
                .createUnit(
                        tenant.getId(),
                        parentUnitId,
                        name,
                        code,
                        type
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