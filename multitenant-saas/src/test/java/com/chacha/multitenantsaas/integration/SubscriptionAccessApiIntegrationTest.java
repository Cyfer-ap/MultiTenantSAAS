package com.chacha.multitenantsaas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chacha.multitenantsaas.dto.SubscriptionPlanCreateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionAccessApiIntegrationTest {

    private static final String ADMIN_PASSWORD = "SubscriptionAdmin@123";

    private static final String MEMBER_PASSWORD = "SubscriptionMember@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Autowired private TenantOnboardingService tenantOnboardingService;

    @Autowired private SubscriptionPlanService subscriptionPlanService;

    @Autowired private TenantSubscriptionService tenantSubscriptionService;

    @Test
    void ordinaryTenantUserReadsOperationalAccessWithoutBillingPermission() throws Exception {
        String suffix = uniqueSuffix();
        TenantOnboardingResponse onboarding =
                onboard(
                        "access-" + suffix,
                        "Access Tenant " + suffix,
                        "access-admin-" + suffix + "@example.com");

        UUID tenantId = onboarding.tenant().id();
        assignActiveSubscription(tenantId, suffix);

        String adminToken = login(tenantId, onboarding.adminUser().email(), ADMIN_PASSWORD);

        String memberEmail = "access-member-" + suffix + "@example.com";

        mockMvc.perform(
                        post("/api/tenants/{tenantId}/users", tenantId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fullName": "Subscription Member",
                                          "email": "%s",
                                          "password": "%s",
                                          "role": "TENANT_USER"
                                        }
                                        """
                                                .formatted(memberEmail, MEMBER_PASSWORD)))
                .andExpect(status().isOk());

        String memberToken = login(tenantId, memberEmail, MEMBER_PASSWORD);

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/subscription", tenantId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/tenants/{tenantId}" + "/subscription/entitlements", tenantId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/tenants/{tenantId}" + "/subscription/access", tenantId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.data.accessLevel").value("FULL_ACCESS"))
                .andExpect(jsonPath("$.data.accessReason").value("ACTIVE"))
                .andExpect(jsonPath("$.data.userCreationAllowed").value(true))
                .andExpect(jsonPath("$.data.projectCreationAllowed").value(true));

        TenantOnboardingResponse otherTenant =
                onboard(
                        "other-" + suffix,
                        "Other Tenant " + suffix,
                        "other-admin-" + suffix + "@example.com");

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}" + "/subscription/access",
                                        otherTenant.tenant().id())
                                .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());
    }

    private TenantOnboardingResponse onboard(String slug, String tenantName, String adminEmail) {
        return tenantOnboardingService.onboardTenant(
                new TenantOnboardingRequest(
                        tenantName, slug, "Subscription Admin", adminEmail, ADMIN_PASSWORD));
    }

    private void assignActiveSubscription(UUID tenantId, String suffix) {
        SubscriptionPlanResponse plan =
                subscriptionPlanService.createPlan(
                        new SubscriptionPlanCreateRequest(
                                "access-" + suffix,
                                "Access Plan " + suffix,
                                "Operational access test plan",
                                BillingInterval.MONTHLY,
                                new BigDecimal("29.00"),
                                "USD",
                                5,
                                3,
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

    private String login(UUID tenantId, String email, String password) throws Exception {
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
                                                        .formatted(email, password)))
                        .andExpect(status().isOk())
                        .andReturn();

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
