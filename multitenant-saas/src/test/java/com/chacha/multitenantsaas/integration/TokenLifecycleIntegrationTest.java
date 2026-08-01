package com.chacha.multitenantsaas.integration;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenLifecycleIntegrationTest {

    private static final String INITIAL_PASSWORD =
            "TenantAdmin@123";

    private static final String NEW_PASSWORD =
            "ChangedTenantAdmin@456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void refreshRotatesTokenAndRejectsPreviousRefreshToken()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("refresh-rotation");

        SessionTokens initialSession =
                login(tenant, INITIAL_PASSWORD);

        SessionTokens refreshedSession =
                refresh(initialSession.refreshToken());

        assertNotEquals(
                initialSession.refreshToken(),
                refreshedSession.refreshToken()
        );

        assertNotEquals(
                initialSession.refreshToken(),
                refreshedSession.refreshToken()
        );

        assertNotNull(refreshedSession.accessToken());
        assertFalse(refreshedSession.accessToken().isBlank());

        assertAccessTokenWorks(
                tenant,
                refreshedSession.accessToken()
        );

        /*
         * Refresh-token rotation means the previous token
         * must no longer be accepted.
         */
        assertRefreshRejected(initialSession.refreshToken());

        /*
         * The newly issued refresh token must remain usable.
         */
        SessionTokens secondRefresh =
                refresh(refreshedSession.refreshToken());

        assertNotNull(secondRefresh.accessToken());
        assertFalse(secondRefresh.accessToken().isBlank());
    }

    @Test
    void logoutRevokesSubmittedRefreshToken()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("logout");

        SessionTokens session =
                login(tenant, INITIAL_PASSWORD);

        String requestBody = refreshTokenRequest(
                session.refreshToken()
        );

        mockMvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message")
                        .value("Logout successful"));

        assertRefreshRejected(session.refreshToken());
    }

    @Test
    void logoutAllRevokesEveryActiveRefreshTokenForUser()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("logout-all");

        /*
         * Simulate the same user being logged in on
         * two different devices.
         */
        SessionTokens firstDevice =
                login(tenant, INITIAL_PASSWORD);

        SessionTokens secondDevice =
                login(tenant, INITIAL_PASSWORD);

        assertNotEquals(
                firstDevice.refreshToken(),
                secondDevice.refreshToken()
        );

        mockMvc.perform(
                        post("/api/auth/logout-all")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer "
                                                + firstDevice.accessToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message")
                        .value(
                                "Logged out from all devices successfully"
                        ));

        assertRefreshRejected(firstDevice.refreshToken());
        assertRefreshRejected(secondDevice.refreshToken());

        assertAccessTokenRejected(firstDevice.accessToken());
        assertAccessTokenRejected(secondDevice.accessToken());
    }

    @Test
    void passwordChangeUpdatesCredentialsAndRevokesAllRefreshTokens()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("password-change");

        SessionTokens firstDevice =
                login(tenant, INITIAL_PASSWORD);

        SessionTokens secondDevice =
                login(tenant, INITIAL_PASSWORD);

        String changePasswordRequest = """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(
                INITIAL_PASSWORD,
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer "
                                                + firstDevice.accessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(changePasswordRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message")
                        .value(
                                "Password changed successfully. "
                                        + "Please login again."
                        ));

        /*
         * Password change must revoke refresh tokens from
         * every active session.
         */
        assertRefreshRejected(firstDevice.refreshToken());
        assertRefreshRejected(secondDevice.refreshToken());

        assertAccessTokenRejected(firstDevice.accessToken());
        assertAccessTokenRejected(secondDevice.accessToken());

        /*
         * The previous password must no longer authenticate.
         */
        assertLoginRejected(
                tenant,
                INITIAL_PASSWORD
        );

        /*
         * The user must be able to log in using the new password.
         */
        SessionTokens newSession =
                login(tenant, NEW_PASSWORD);

        assertNotNull(newSession.accessToken());
        assertFalse(newSession.accessToken().isBlank());
        assertNotNull(newSession.refreshToken());
        assertFalse(newSession.refreshToken().isBlank());
    }

    private TenantFixture onboardUniqueTenant(String prefix)
            throws Exception {

        String suffix = uniqueSuffix();
        String slug = prefix + "-" + suffix;
        String email =
                "admin." + suffix + "@example.test";

        String requestBody = """
                {
                  "tenantName": "%s",
                  "tenantSlug": "%s",
                  "adminFullName": "Token Test Administrator",
                  "adminEmail": "%s",
                  "adminPassword": "%s"
                }
                """.formatted(
                prefix + " Tenant",
                slug,
                email,
                INITIAL_PASSWORD
        );

        MvcResult result = mockMvc.perform(
                        post("/api/onboarding/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenant.slug")
                        .value(slug))
                .andExpect(jsonPath("$.data.adminUser.email")
                        .value(email))
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        UUID tenantId = UUID.fromString(
                response.at("/data/tenant/id").asString()
        );

        return new TenantFixture(
                tenantId,
                email
        );
    }

    private SessionTokens login(
            TenantFixture tenant,
            String password
    ) throws Exception {

        String requestBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                tenant.adminEmail(),
                password
        );

        MvcResult result = mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/auth/login",
                                tenant.tenantId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId")
                        .value(tenant.tenantId().toString()))
                .andExpect(jsonPath("$.data.email")
                        .value(tenant.adminEmail()))
                .andExpect(jsonPath("$.data.role")
                        .value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.data.accessToken")
                        .isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken")
                        .isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType")
                        .value("Bearer"))
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return new SessionTokens(
                response.at("/data/accessToken").asString(),
                response.at("/data/refreshToken").asString()
        );
    }

    private SessionTokens refresh(String refreshToken)
            throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        refreshTokenRequest(refreshToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken")
                        .isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken")
                        .isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType")
                        .value("Bearer"))
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return new SessionTokens(
                response.at("/data/accessToken").asString(),
                response.at("/data/refreshToken").asString()
        );
    }

    private void assertRefreshRejected(String refreshToken)
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        refreshTokenRequest(refreshToken)
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode")
                        .value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path")
                        .value("/api/auth/refresh"));
    }

    private void assertLoginRejected(
            TenantFixture tenant,
            String password
    ) throws Exception {

        String requestBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(
                tenant.adminEmail(),
                password
        );

        String expectedPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/auth/login";

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/auth/login",
                                tenant.tenantId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode")
                        .value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path")
                        .value(expectedPath));
    }

    private String refreshTokenRequest(String refreshToken) {
        return """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }

    private void assertAccessTokenRejected(
            String accessToken
    ) throws Exception {
        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path")
                        .value("/api/auth/me"));
    }

    private record TenantFixture(
            UUID tenantId,
            String adminEmail
    ) {
    }

    private record SessionTokens(
            String accessToken,
            String refreshToken
    ) {
    }

    private void assertAccessTokenWorks(
            TenantFixture tenant,
            String accessToken
    ) throws Exception {

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId")
                        .value(tenant.tenantId().toString()))
                .andExpect(jsonPath("$.data.email")
                        .value(tenant.adminEmail()))
                .andExpect(jsonPath("$.data.role")
                        .value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.data.status")
                        .value("ACTIVE"));
    }
}