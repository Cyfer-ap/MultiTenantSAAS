package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
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

import java.util.UUID;

import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationV2CutoverIntegrationTest {

    private static final String PASSWORD =
            "AuthorizationCutover@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TenantOnboardingService
            tenantOnboardingService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void legacyRoleIsDeniedUntilV2BackfillRuns()
            throws Exception {

        TestContext context =
                createContext();

        /*
         * This user deliberately bypasses AppUserService,
         * invitation acceptance and tenant onboarding.
         *
         * Therefore, the user has a legacy TENANT_ADMIN role
         * but no Authorization V2 role assignment.
         */
        AppUser unprovisionedLegacyAdministrator =
                createLegacyAdministrator(
                        context.tenant()
                );

        String provisionedAdministratorToken =
                login(
                        context.tenant().getId(),
                        context.administrator().getEmail()
                );

        String legacyAdministratorToken =
                login(
                        context.tenant().getId(),
                        unprovisionedLegacyAdministrator
                                .getEmail()
                );

        /*
         * A legacy role by itself no longer grants tenant.read.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                legacyAdministratorToken
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        /*
         * It also cannot manage Authorization V2 or run its
         * own backfill.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization"
                                        + "/permissions",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                legacyAdministratorToken
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/authorization"
                                        + "/provisioning"
                                        + "/backfill",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                legacyAdministratorToken
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        /*
         * The already provisioned administrator repairs the
         * tenant.
         */
        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/authorization"
                                        + "/provisioning"
                                        + "/backfill",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                provisionedAdministratorToken
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.provisioning"
                                        + ".assignmentsCreated"
                        )
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.data.readiness.ready"
                        )
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.data.readiness"
                                        + ".unresolvedUsers"
                        )
                                .value(0)
                );

        /*
         * Authorization is database-driven, so the existing
         * JWT becomes authorized after the role assignment is
         * created. A new login is not required.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                legacyAdministratorToken
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.id")
                                .value(
                                        context.tenant()
                                                .getId()
                                                .toString()
                                )
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization"
                                        + "/permissions",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                legacyAdministratorToken
                                        )
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization"
                                        + "/provisioning"
                                        + "/readiness",
                                context.tenant().getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(
                                                legacyAdministratorToken
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.ready")
                                .value(true)
                );
    }

    private TestContext createContext() {
        String suffix = uniqueSuffix();

        TenantOnboardingResponse onboarding =
                tenantOnboardingService
                        .onboardTenant(
                                new TenantOnboardingRequest(
                                        "Cutover Tenant",
                                        "cutover-" + suffix,
                                        "Provisioned Administrator",
                                        "cutover.admin."
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

    private AppUser createLegacyAdministrator(
            Tenant tenant
    ) {
        String suffix = uniqueSuffix();

        AppUser user =
                new AppUser(
                        tenant,
                        "Unprovisioned Legacy Administrator",
                        "legacy.admin."
                                + suffix
                                + "@example.test",
                        passwordEncoder.encode(PASSWORD),
                        UserRole.TENANT_ADMIN
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

        JsonNode response =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return response
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