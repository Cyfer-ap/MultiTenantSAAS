package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chacha.multitenantsaas.billing.service.BillingUsageMeteringService;
import com.chacha.multitenantsaas.dto.TenantApiKeyCreateRequest;
import com.chacha.multitenantsaas.dto.TenantApiKeyCreatedResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantApiKeyRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.TenantApiKeyAuthenticationFilter;
import com.chacha.multitenantsaas.service.TenantApiKeyService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TenantApiKeyAuthenticationIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private TenantApiKeyService apiKeyService;

    @Autowired private TenantApiKeyRepository apiKeyRepository;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private BillingUsageMeteringService usageMeteringService;

    @Test
    void authenticatesAttributesAndMetersExternalApiRequest() throws Exception {
        Tenant tenant = createTenant("external");
        AppUser actor = createAdmin(tenant);
        TenantApiKeyCreatedResponse created =
                apiKeyService.create(
                        tenant.getId(),
                        actor,
                        new TenantApiKeyCreateRequest("External integration", null));
        Instant periodStart = Instant.now().minusSeconds(60);

        mockMvc.perform(
                        get("/api/external/v1/context")
                                .header(
                                        TenantApiKeyAuthenticationFilter.API_KEY_HEADER,
                                        created.apiKey()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.tenantId").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.data.apiKeyId").value(created.id().toString()))
                .andExpect(jsonPath("$.data.apiKeyName").value("External integration"))
                .andExpect(jsonPath("$.data.keyPrefix").value(created.keyPrefix()));

        Instant periodEnd = Instant.now().plusSeconds(60);
        var summary =
                usageMeteringService.summarize(
                        tenant.getId(), "API_REQUESTS", periodStart, periodEnd);

        assertThat(summary.quantity()).isEqualTo(1L);
        assertThat(summary.eventCount()).isEqualTo(1L);
        assertThat(apiKeyRepository.findById(created.id()).orElseThrow().getLastUsedAt())
                .isNotNull();
    }

    @Test
    void rejectsInvalidAndRevokedKeysWithoutMeteringThem() throws Exception {
        Tenant tenant = createTenant("revoked");
        AppUser actor = createAdmin(tenant);
        TenantApiKeyCreatedResponse created =
                apiKeyService.create(
                        tenant.getId(), actor, new TenantApiKeyCreateRequest("Revocable", null));
        Instant periodStart = Instant.now().minusSeconds(60);

        mockMvc.perform(
                        get("/api/external/v1/context")
                                .header(
                                        TenantApiKeyAuthenticationFilter.API_KEY_HEADER,
                                        created.apiKey() + "invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message").value("Unauthorized. Missing or invalid API key."));

        apiKeyService.revoke(tenant.getId(), created.id(), actor);

        mockMvc.perform(
                        get("/api/external/v1/context")
                                .header(
                                        TenantApiKeyAuthenticationFilter.API_KEY_HEADER,
                                        created.apiKey()))
                .andExpect(status().isUnauthorized());

        var summary =
                usageMeteringService.summarize(
                        tenant.getId(), "API_REQUESTS", periodStart, Instant.now().plusSeconds(60));

        assertThat(summary.quantity()).isZero();
        assertThat(summary.eventCount()).isZero();
    }

    @Test
    void externalApiRequiresApiKeyAuthority() throws Exception {
        mockMvc.perform(get("/api/external/v1/context"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message").value("Unauthorized. Missing or invalid API key."));
    }

    @Test
    void apiKeyCannotAuthenticateExistingJwtEndpoints() throws Exception {
        Tenant tenant = createTenant("jwt-boundary");
        AppUser actor = createAdmin(tenant);
        TenantApiKeyCreatedResponse created =
                apiKeyService.create(
                        tenant.getId(),
                        actor,
                        new TenantApiKeyCreateRequest("External only", null));

        mockMvc.perform(
                        get("/api/tenants/" + tenant.getId())
                                .header(
                                        TenantApiKeyAuthenticationFilter.API_KEY_HEADER,
                                        created.apiKey()))
                .andExpect(status().isUnauthorized());
    }

    private Tenant createTenant(String prefix) {
        return tenantRepository.saveAndFlush(
                new Tenant(
                        "External API Labs",
                        prefix + "-" + UUID.randomUUID().toString().substring(0, 8)));
    }

    private AppUser createAdmin(Tenant tenant) {
        return appUserRepository.saveAndFlush(
                new AppUser(
                        tenant,
                        "External API Admin",
                        "admin-" + UUID.randomUUID() + "@example.com",
                        "not-used",
                        UserRole.TENANT_ADMIN));
    }
}
