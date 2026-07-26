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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantDashboardIntegrationTest {

    private static final String ADMIN_PASSWORD =
            "TenantAdmin@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void newlyOnboardedTenantHasCorrectEmptyDashboardSummary()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("dashboard-empty");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail()
        );

        mockMvc.perform(
                        get("/api/tenant/dashboard/summary")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success")
                        .value(true))
                .andExpect(jsonPath("$.data.tenantId")
                        .value(tenant.tenantId().toString()))
                .andExpect(jsonPath("$.data.tenantName")
                        .value(tenant.tenantName()))
                .andExpect(jsonPath("$.data.tenantSlug")
                        .value(tenant.tenantSlug()))
                .andExpect(jsonPath("$.data.tenantStatus")
                        .value("ACTIVE"))

                .andExpect(jsonPath("$.data.totalUsers")
                        .value(1))
                .andExpect(jsonPath("$.data.activeUsers")
                        .value(1))
                .andExpect(jsonPath("$.data.inactiveUsers")
                        .value(0))
                .andExpect(jsonPath("$.data.suspendedUsers")
                        .value(0))

                .andExpect(jsonPath("$.data.totalProjects")
                        .value(0))
                .andExpect(jsonPath("$.data.planningProjects")
                        .value(0))
                .andExpect(jsonPath("$.data.activeProjects")
                        .value(0))
                .andExpect(jsonPath("$.data.onHoldProjects")
                        .value(0))
                .andExpect(jsonPath("$.data.completedProjects")
                        .value(0))
                .andExpect(jsonPath("$.data.archivedProjects")
                        .value(0))

                .andExpect(jsonPath("$.data.totalProjectMemberships")
                        .value(0))

                .andExpect(jsonPath("$.data.totalTasks")
                        .value(0))
                .andExpect(jsonPath("$.data.todoTasks")
                        .value(0))
                .andExpect(jsonPath("$.data.inProgressTasks")
                        .value(0))
                .andExpect(jsonPath("$.data.blockedTasks")
                        .value(0))
                .andExpect(jsonPath("$.data.completedTasks")
                        .value(0))
                .andExpect(jsonPath("$.data.cancelledTasks")
                        .value(0))
                .andExpect(jsonPath("$.data.overdueTasks")
                        .value(0))
                .andExpect(jsonPath(
                        "$.data.taskCompletionPercentage"
                ).value(0.0));
    }

    private TenantFixture onboardUniqueTenant(String prefix)
            throws Exception {

        String suffix = uniqueSuffix();
        String tenantName = prefix + " Tenant";
        String tenantSlug = prefix + "-" + suffix;
        String adminEmail =
                "admin." + suffix + "@example.test";

        String requestBody = """
                {
                  "tenantName": "%s",
                  "tenantSlug": "%s",
                  "adminFullName": "Dashboard Test Administrator",
                  "adminEmail": "%s",
                  "adminPassword": "%s"
                }
                """.formatted(
                tenantName,
                tenantSlug,
                adminEmail,
                ADMIN_PASSWORD
        );

        MvcResult result = mockMvc.perform(
                        post("/api/onboarding/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return new TenantFixture(
                UUID.fromString(
                        response.at("/data/tenant/id").asString()
                ),
                tenantName,
                tenantSlug,
                adminEmail
        );
    }

    private String login(
            UUID tenantId,
            String email
    ) throws Exception {

        String requestBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/auth/login",
                                tenantId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return response.at("/data/accessToken").asString();
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }

    private record TenantFixture(
            UUID tenantId,
            String tenantName,
            String tenantSlug,
            String adminEmail
    ) {
    }
}