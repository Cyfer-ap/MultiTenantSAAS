package com.chacha.multitenantsaas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantOnboardingAndIsolationIntegrationTest {

    private static final String ADMIN_PASSWORD = "TenantAdmin@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Test
    void publicOnboardingCreatesActiveTenantAndTenantAdmin() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("alpha");

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/users", tenant.tenantId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(tenant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value(tenant.adminEmail()))
                .andExpect(jsonPath("$.data.content[0].role").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    @Test
    void duplicateTenantSlugReturnsConflict() throws Exception {

        String suffix = uniqueSuffix();
        String tenantSlug = "duplicate-" + suffix;

        onboardTenant("First Duplicate Tenant", tenantSlug, "first." + suffix + "@example.test");

        String secondRequest =
                onboardingRequest(
                        "Second Duplicate Tenant",
                        tenantSlug,
                        "second." + suffix + "@example.test");

        mockMvc.perform(
                        post("/api/onboarding/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/onboarding/tenants"));
    }

    @Test
    void onboardedTenantAdminCanLogin() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("login");

        String accessToken = login(tenant);

        org.junit.jupiter.api.Assertions.assertNotNull(accessToken);
        org.junit.jupiter.api.Assertions.assertFalse(accessToken.isBlank());
    }

    @Test
    void tenantAdminCanAccessOwnTenantUsers() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("own-access");
        String accessToken = login(tenant);

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/users", tenant.tenantId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value(tenant.adminEmail()));
    }

    @Test
    void tenantAdminCannotAccessAnotherTenantsUsers() throws Exception {

        TenantFixture tenantA = onboardUniqueTenant("tenant-a");
        TenantFixture tenantB = onboardUniqueTenant("tenant-b");

        String tenantAAccessToken = login(tenantA);

        String expectedPath = "/api/tenants/" + tenantB.tenantId() + "/users";

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/users", tenantB.tenantId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value(expectedPath));
    }

    @Test
    void tenantUserCannotLoginThroughAnotherTenant() throws Exception {

        TenantFixture tenantA = onboardUniqueTenant("login-a");
        TenantFixture tenantB = onboardUniqueTenant("login-b");

        String requestBody = loginRequest(tenantA.adminEmail(), tenantA.adminPassword());

        String expectedPath = "/api/tenants/" + tenantB.tenantId() + "/auth/login";

        mockMvc.perform(
                        post("/api/tenants/{tenantId}/auth/login", tenantB.tenantId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(expectedPath));
    }

    private TenantFixture onboardUniqueTenant(String prefix) throws Exception {

        String suffix = uniqueSuffix();

        return onboardTenant(
                prefix + " Tenant", prefix + "-" + suffix, "admin." + suffix + "@example.test");
    }

    private TenantFixture onboardTenant(String tenantName, String tenantSlug, String adminEmail)
            throws Exception {

        String requestBody = onboardingRequest(tenantName, tenantSlug, adminEmail);

        MvcResult result =
                mockMvc.perform(
                                post("/api/onboarding/tenants")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.tenant.slug").value(tenantSlug))
                        .andExpect(jsonPath("$.data.tenant.status").value("ACTIVE"))
                        .andExpect(jsonPath("$.data.adminUser.email").value(adminEmail))
                        .andExpect(jsonPath("$.data.adminUser.role").value("TENANT_ADMIN"))
                        .andExpect(jsonPath("$.data.adminUser.status").value("ACTIVE"))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        UUID tenantId = UUID.fromString(response.at("/data/tenant/id").asString());

        return new TenantFixture(tenantId, tenantSlug, adminEmail, ADMIN_PASSWORD);
    }

    private String login(TenantFixture tenant) throws Exception {

        String requestBody = loginRequest(tenant.adminEmail(), tenant.adminPassword());

        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/auth/login", tenant.tenantId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.tenantId").value(tenant.tenantId().toString()))
                        .andExpect(jsonPath("$.data.email").value(tenant.adminEmail()))
                        .andExpect(jsonPath("$.data.role").value("TENANT_ADMIN"))
                        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return response.at("/data/accessToken").asString();
    }

    private String onboardingRequest(String tenantName, String tenantSlug, String adminEmail) {
        return """
                {
                  "tenantName": "%s",
                  "tenantSlug": "%s",
                  "adminFullName": "Test Tenant Administrator",
                  "adminEmail": "%s",
                  "adminPassword": "%s"
                }
                """
                .formatted(tenantName, tenantSlug, adminEmail, ADMIN_PASSWORD);
    }

    private String loginRequest(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """
                .formatted(email, password);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record TenantFixture(
            UUID tenantId, String tenantSlug, String adminEmail, String adminPassword) {}
}
