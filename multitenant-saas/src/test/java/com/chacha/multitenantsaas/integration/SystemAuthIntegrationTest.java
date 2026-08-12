package com.chacha.multitenantsaas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemAuthIntegrationTest {

    private static final String SYSTEM_ADMIN_EMAIL = "system.test@saas.local";

    private static final String SYSTEM_ADMIN_PASSWORD = "TestSystemAdmin@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Test
    void missingAccessTokenReturnsStandardizedUnauthorizedResponse() throws Exception {

        mockMvc.perform(get("/api/system/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/system/auth/me"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidLoginCredentialsReturnStandardizedAuthenticationError() throws Exception {

        String requestBody =
                """
                {
                  "email": "system.test@saas.local",
                  "password": "WrongPassword@123"
                }
                """;

        mockMvc.perform(
                        post("/api/system/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/system/auth/login"));
    }

    @Test
    void invalidLoginRequestReturnsFieldValidationErrors() throws Exception {

        String requestBody =
                """
                {
                  "email": "not-an-email",
                  "password": ""
                }
                """;

        mockMvc.perform(
                        post("/api/system/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.email").value("Email must be valid"))
                .andExpect(jsonPath("$.details.password").value("Password is required"));
    }

    @Test
    void validLoginReturnsAccessTokenAndAllowsAccessToCurrentAdmin() throws Exception {

        String loginRequestBody =
                """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """
                        .formatted(SYSTEM_ADMIN_EMAIL, SYSTEM_ADMIN_PASSWORD);

        String loginResponse =
                mockMvc.perform(
                                post("/api/system/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(loginRequestBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.email").value(SYSTEM_ADMIN_EMAIL))
                        .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMIN"))
                        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String accessToken = jsonMapper.readTree(loginResponse).at("/data/accessToken").asString();

        mockMvc.perform(
                        get("/api/system/auth/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(SYSTEM_ADMIN_EMAIL))
                .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMIN"));
    }
}
