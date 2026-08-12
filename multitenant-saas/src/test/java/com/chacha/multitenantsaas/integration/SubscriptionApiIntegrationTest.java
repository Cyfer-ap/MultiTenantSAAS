package com.chacha.multitenantsaas.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.PlatformAuditAction;
import com.chacha.multitenantsaas.repository.AuditLogRepository;
import com.chacha.multitenantsaas.repository.PlatformAuditLogRepository;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionApiIntegrationTest {

    private static final String SYSTEM_ADMIN_EMAIL = "system.test@saas.local";

    private static final String SYSTEM_ADMIN_PASSWORD = "TestSystemAdmin@123";

    private static final String TENANT_ADMIN_PASSWORD = "SubscriptionAdmin@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Autowired private TenantOnboardingService tenantOnboardingService;

    @Autowired private PlatformAuditLogRepository platformAuditLogRepository;

    @Autowired private AuditLogRepository auditLogRepository;

    @Test
    void systemAdminManagesPlansAndTenantReadsSubscription() throws Exception {

        String suffix = uniqueSuffix();

        TenantOnboardingResponse onboarding =
                tenantOnboardingService.onboardTenant(
                        new TenantOnboardingRequest(
                                "Subscription Tenant " + suffix,
                                "subscription-tenant-" + suffix,
                                "Subscription Admin",
                                "subscription-admin-" + suffix + "@example.com",
                                TENANT_ADMIN_PASSWORD));

        UUID tenantId = onboarding.tenant().id();

        String systemToken = loginSystemAdmin();

        UUID starterPlanId =
                createPlan(systemToken, "starter-" + suffix, "Starter " + suffix, "29.00");

        UUID growthPlanId =
                createPlan(systemToken, "growth-" + suffix, "Growth " + suffix, "79.00");

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        mockMvc.perform(
                        post("/api/system/tenants/{tenantId}" + "/subscription", tenantId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(systemToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "planId": "%s",
                                          "status": "ACTIVE",
                                          "startedAt": "%s",
                                          "currentPeriodStart": "%s",
                                          "currentPeriodEnd": "%s",
                                          "trialEndsAt": null,
                                          "cancelAtPeriodEnd": false
                                        }
                                        """
                                                .formatted(
                                                        starterPlanId,
                                                        now,
                                                        now,
                                                        now.plus(30, ChronoUnit.DAYS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan.id").value(starterPlanId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        String tenantToken = loginTenantAdmin(tenantId, onboarding.adminUser().email());

        mockMvc.perform(
                        get("/api/tenants/{tenantId}" + "/subscription", tenantId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(tenantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.data.plan.id").value(starterPlanId.toString()));

        mockMvc.perform(
                        put("/api/system/tenants/{tenantId}" + "/subscription/plan", tenantId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(systemToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "planId": "%s",
                                          "currentPeriodStart": "%s",
                                          "currentPeriodEnd": "%s"
                                        }
                                        """
                                                .formatted(
                                                        growthPlanId,
                                                        now.plus(30, ChronoUnit.DAYS),
                                                        now.plus(395, ChronoUnit.DAYS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan.id").value(growthPlanId.toString()));

        mockMvc.perform(
                        patch(
                                        "/api/system/tenants/{tenantId}"
                                                + "/subscription/lifecycle",
                                        tenantId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(systemToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "status": "ACTIVE",
                                          "cancelAtPeriodEnd": true,
                                          "currentPeriodEnd": null,
                                          "trialEndsAt": null
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelAtPeriodEnd").value(true));

        mockMvc.perform(
                        get("/api/system/subscription-plans")
                                .header(HttpHeaders.AUTHORIZATION, bearer(systemToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        assertTrue(
                platformAuditLogRepository.findAll().stream()
                        .anyMatch(
                                log ->
                                        log.getAction()
                                                == PlatformAuditAction.SUBSCRIPTION_PLAN_CREATED));

        assertTrue(
                platformAuditLogRepository.findAll().stream()
                        .anyMatch(
                                log ->
                                        log.getAction()
                                                == PlatformAuditAction
                                                        .TENANT_SUBSCRIPTION_PLAN_CHANGED));

        assertTrue(
                auditLogRepository.findAll().stream()
                        .anyMatch(
                                log ->
                                        log.getTenant().getId().equals(tenantId)
                                                && log.getAction()
                                                        == AuditAction
                                                                .TENANT_SUBSCRIPTION_LIFECYCLE_UPDATED));
    }

    @Test
    void systemSubscriptionApisRejectMissingAuthentication() throws Exception {

        mockMvc.perform(get("/api/system/subscription-plans")).andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/system/tenants/{tenantId}" + "/subscription", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private UUID createPlan(String token, String code, String name, String price) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/system" + "/subscription-plans")
                                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "code": "%s",
                                                  "name": "%s",
                                                  "description": "Integration test plan",
                                                  "billingInterval": "MONTHLY",
                                                  "price": %s,
                                                  "currency": "USD",
                                                  "maxUsers": 25,
                                                  "maxProjects": 50,
                                                  "maxStorageMb": 4096
                                                }
                                                """
                                                        .formatted(code, name, price)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andReturn();

        return UUID.fromString(
                jsonMapper
                        .readTree(result.getResponse().getContentAsString())
                        .at("/data/id")
                        .asString());
    }

    private String loginSystemAdmin() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/system/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "email": "%s",
                                                  "password": "%s"
                                                }
                                                """
                                                        .formatted(
                                                                SYSTEM_ADMIN_EMAIL,
                                                                SYSTEM_ADMIN_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();

        return readAccessToken(result);
    }

    private String loginTenantAdmin(UUID tenantId, String email) throws Exception {
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
                                                        .formatted(email, TENANT_ADMIN_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();

        return readAccessToken(result);
    }

    private String readAccessToken(MvcResult result) throws Exception {
        JsonNode body = jsonMapper.readTree(result.getResponse().getContentAsString());

        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
