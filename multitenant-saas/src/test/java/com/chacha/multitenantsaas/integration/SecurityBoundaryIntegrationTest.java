package com.chacha.multitenantsaas.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityBoundaryIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void publicHealthRemainsAvailableWithSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/health").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(
                        header().string(
                                        "Permissions-Policy",
                                        "camera=(), microphone=(), geolocation=()"))
                .andExpect(
                        header().string("Strict-Transport-Security", containsString("max-age=")));
    }

    @Test
    void billingWebhookPostsReachSignatureValidationWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        post("/api/billing/webhooks/stripe")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/api/billing/webhooks/razorpay")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void neighboringBillingWebhookRoutesRemainProtected() throws Exception {
        mockMvc.perform(
                        post("/api/billing/webhooks/not-a-provider")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/billing/webhooks/stripe")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownBackendRouteFailsClosed() throws Exception {
        mockMvc.perform(get("/not-a-real-backend-route")).andExpect(status().isUnauthorized());
    }

    @Test
    void h2ConsoleIsNeverPublic() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }

    @Test
    void openApiDocumentationIsNotPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }
}
