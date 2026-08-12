package com.chacha.multitenantsaas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chacha.multitenantsaas.dto.SubscriptionPlanCreateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionLifecycleUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import com.chacha.multitenantsaas.service.TenantSubscriptionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "app.subscription.enforcement.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionQuotaEnforcementIntegrationTest {

    private static final String ADMIN_PASSWORD = "QuotaAdmin@123";
    private static final String USER_PASSWORD = "QuotaUser@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Autowired private SubscriptionPlanService subscriptionPlanService;

    @Autowired private TenantSubscriptionService tenantSubscriptionService;

    @Test
    void enforcesQuotaGrowthAndStillAllowsCleanup() throws Exception {
        TenantFixture tenant = onboardTenant();
        attachSubscription(tenant.tenantId());

        String adminToken = login(tenant.tenantId(), tenant.adminEmail());

        createUser(tenant.tenantId(), adminToken, "Quota User A");
        UUID userB = createUser(tenant.tenantId(), adminToken, "Quota User B");

        assertUserCreationBlocked(
                tenant.tenantId(), adminToken, "Quota Overflow", "USER_LIMIT_REACHED", null);

        deactivateUser(tenant.tenantId(), userB, adminToken);

        UUID userC = createUser(tenant.tenantId(), adminToken, "Quota User C");

        mockMvc.perform(
                        patch(
                                        "/api/tenants/{tenantId}" + "/users/{userId}/status",
                                        tenant.tenantId(),
                                        userB)
                                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "status": "ACTIVE"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.restriction").value("USER_LIMIT_REACHED"))
                .andExpect(jsonPath("$.details.used").value("3"))
                .andExpect(jsonPath("$.details.limit").value("3"));

        String invitationToken = createInvitation(tenant.tenantId(), adminToken);

        mockMvc.perform(
                        post("/api/user-invitations/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "invitationToken": "%s",
                                          "newPassword": "%s",
                                          "confirmPassword": "%s"
                                        }
                                        """
                                                .formatted(
                                                        invitationToken,
                                                        USER_PASSWORD,
                                                        USER_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.restriction").value("USER_LIMIT_REACHED"));

        UUID projectA = createProject(tenant.tenantId(), adminToken, "Quota Project A");

        assertProjectCreationBlocked(
                tenant.tenantId(),
                adminToken,
                "Quota Project Overflow",
                "PROJECT_LIMIT_REACHED",
                null);

        archiveProject(tenant.tenantId(), projectA, adminToken);

        UUID projectB = createProject(tenant.tenantId(), adminToken, "Quota Project B");

        tenantSubscriptionService.updateLifecycle(
                tenant.tenantId(),
                new TenantSubscriptionLifecycleUpdateRequest(
                        TenantSubscriptionStatus.CANCELLED, false, null, null));

        /* Cleanup operations remain available while growth is blocked. */
        deactivateUser(tenant.tenantId(), userC, adminToken);
        archiveProject(tenant.tenantId(), projectB, adminToken);

        assertUserCreationBlocked(
                tenant.tenantId(),
                adminToken,
                "Cancelled User",
                "WORKSPACE_READ_ONLY",
                "CANCELLED");

        assertProjectCreationBlocked(
                tenant.tenantId(),
                adminToken,
                "Cancelled Project",
                "WORKSPACE_READ_ONLY",
                "CANCELLED");
    }

    private TenantFixture onboardTenant() throws Exception {
        String suffix = uniqueSuffix();
        String adminEmail = "quota.admin." + suffix + "@example.test";
        String slug = "quota-" + suffix;

        MvcResult result =
                mockMvc.perform(
                                post("/api/onboarding/tenants")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                        {
                                          "tenantName": "Quota Tenant %s",
                                          "tenantSlug": "%s",
                                          "adminFullName": "Quota Administrator",
                                          "adminEmail": "%s",
                                          "adminPassword": "%s"
                                        }
                                        """
                                                        .formatted(
                                                                suffix,
                                                                slug,
                                                                adminEmail,
                                                                ADMIN_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return new TenantFixture(
                UUID.fromString(response.at("/data/tenant/id").asString()), adminEmail);
    }

    private void attachSubscription(UUID tenantId) {
        String suffix = uniqueSuffix();
        SubscriptionPlanResponse plan =
                subscriptionPlanService.createPlan(
                        new SubscriptionPlanCreateRequest(
                                "quota_" + suffix,
                                "Quota Plan " + suffix,
                                "Quota enforcement integration plan",
                                BillingInterval.MONTHLY,
                                BigDecimal.ZERO,
                                "USD",
                                3,
                                1,
                                1024L));

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        tenantSubscriptionService.startSubscription(
                tenantId,
                new TenantSubscriptionStartRequest(
                        plan.id(),
                        TenantSubscriptionStatus.ACTIVE,
                        now,
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        null,
                        false));
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
                                                        .formatted(email, ADMIN_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return response.at("/data/accessToken").asString();
    }

    private UUID createUser(UUID tenantId, String token, String fullName) throws Exception {
        String email = emailFor(fullName);

        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/users", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(userRequest(fullName, email)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return UUID.fromString(response.at("/data/id").asString());
    }

    private void assertUserCreationBlocked(
            UUID tenantId, String token, String fullName, String restriction, String accessReason)
            throws Exception {
        var action =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/users", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(userRequest(fullName, emailFor(fullName))))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                        .andExpect(jsonPath("$.details.restriction").value(restriction))
                        .andExpect(
                                jsonPath("$.details.resource")
                                        .value(
                                                "WORKSPACE_READ_ONLY".equals(restriction)
                                                        ? "workspace"
                                                        : "users"));

        if (accessReason != null) {
            action.andExpect(jsonPath("$.details.accessReason").value(accessReason));
        }
    }

    private void deactivateUser(UUID tenantId, UUID userId, String token) throws Exception {
        mockMvc.perform(
                        delete("/api/tenants/{tenantId}/users/{userId}", tenantId, userId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private String createInvitation(UUID tenantId, String token) throws Exception {
        String suffix = uniqueSuffix();

        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/user-invitations", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                        {
                                          "fullName": "Invited Quota User",
                                          "email": "invited.%s@example.test",
                                          "role": "TENANT_USER"
                                        }
                                        """
                                                        .formatted(suffix)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return response.at("/data/devInvitationToken").asString();
    }

    private UUID createProject(UUID tenantId, String token, String name) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/projects", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(projectRequest(name)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return UUID.fromString(response.at("/data/id").asString());
    }

    private void assertProjectCreationBlocked(
            UUID tenantId, String token, String name, String restriction, String accessReason)
            throws Exception {
        var action =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/projects", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(projectRequest(name)))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                        .andExpect(jsonPath("$.details.restriction").value(restriction))
                        .andExpect(
                                jsonPath("$.details.resource")
                                        .value(
                                                "WORKSPACE_READ_ONLY".equals(restriction)
                                                        ? "workspace"
                                                        : "projects"));

        if (accessReason != null) {
            action.andExpect(jsonPath("$.details.accessReason").value(accessReason));
        }
    }

    private void archiveProject(UUID tenantId, UUID projectId, String token) throws Exception {
        mockMvc.perform(
                        delete("/api/tenants/{tenantId}/projects/{projectId}", tenantId, projectId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private String userRequest(String fullName, String email) {
        return """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "password": "%s",
                  "role": "TENANT_USER"
                }
                """
                .formatted(fullName, email, USER_PASSWORD);
    }

    private String projectRequest(String name) {
        return """
                {
                  "name": "%s",
                  "description": "Quota enforcement test project"
                }
                """
                .formatted(name);
    }

    private String emailFor(String fullName) {
        return fullName.toLowerCase().replace(' ', '.') + "." + uniqueSuffix() + "@example.test";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record TenantFixture(UUID tenantId, String adminEmail) {}
}
