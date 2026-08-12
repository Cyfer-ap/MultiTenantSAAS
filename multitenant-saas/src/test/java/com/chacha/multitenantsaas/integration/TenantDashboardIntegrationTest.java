package com.chacha.multitenantsaas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantDashboardIntegrationTest {

    private static final String ADMIN_PASSWORD = "TenantAdmin@123";

    private static final String INVITED_USER_PASSWORD = "InvitedUser@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Test
    void newlyOnboardedTenantHasCorrectEmptyDashboardSummary() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("dashboard-empty");

        String adminToken = login(tenant.tenantId(), tenant.adminEmail());

        mockMvc.perform(
                        get("/api/tenant/dashboard/summary")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(tenant.tenantId().toString()))
                .andExpect(jsonPath("$.data.tenantName").value(tenant.tenantName()))
                .andExpect(jsonPath("$.data.tenantSlug").value(tenant.tenantSlug()))
                .andExpect(jsonPath("$.data.tenantStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.totalUsers").value(1))
                .andExpect(jsonPath("$.data.activeUsers").value(1))
                .andExpect(jsonPath("$.data.inactiveUsers").value(0))
                .andExpect(jsonPath("$.data.suspendedUsers").value(0))
                .andExpect(jsonPath("$.data.totalProjects").value(0))
                .andExpect(jsonPath("$.data.planningProjects").value(0))
                .andExpect(jsonPath("$.data.activeProjects").value(0))
                .andExpect(jsonPath("$.data.onHoldProjects").value(0))
                .andExpect(jsonPath("$.data.completedProjects").value(0))
                .andExpect(jsonPath("$.data.archivedProjects").value(0))
                .andExpect(jsonPath("$.data.totalProjectMemberships").value(0))
                .andExpect(jsonPath("$.data.totalTasks").value(0))
                .andExpect(jsonPath("$.data.todoTasks").value(0))
                .andExpect(jsonPath("$.data.inProgressTasks").value(0))
                .andExpect(jsonPath("$.data.blockedTasks").value(0))
                .andExpect(jsonPath("$.data.completedTasks").value(0))
                .andExpect(jsonPath("$.data.cancelledTasks").value(0))
                .andExpect(jsonPath("$.data.overdueTasks").value(0))
                .andExpect(jsonPath("$.data.taskCompletionPercentage").value(0.0));
    }

    @Test
    void populatedTenantHasCorrectDashboardMetrics() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("dashboard-populated");

        String adminToken = login(tenant.tenantId(), tenant.adminEmail());

        UUID planningProjectId = createProject(tenant.tenantId(), adminToken, "Planning Project");

        UUID activeProjectId = createProject(tenant.tenantId(), adminToken, "Active Project");

        UUID onHoldProjectId = createProject(tenant.tenantId(), adminToken, "On Hold Project");

        UUID completedProjectId = createProject(tenant.tenantId(), adminToken, "Completed Project");

        UUID archivedProjectId = createProject(tenant.tenantId(), adminToken, "Archived Project");

        updateProjectStatus(tenant.tenantId(), activeProjectId, adminToken, "ACTIVE");

        updateProjectStatus(tenant.tenantId(), onHoldProjectId, adminToken, "ON_HOLD");

        updateProjectStatus(tenant.tenantId(), completedProjectId, adminToken, "COMPLETED");

        archiveProject(tenant.tenantId(), archivedProjectId, adminToken);

        Instant overdueDueAt = Instant.now().minus(1, ChronoUnit.DAYS);

        Instant futureDueAt = Instant.now().plus(30, ChronoUnit.DAYS);

        createTask(
                tenant.tenantId(), activeProjectId, adminToken, "Overdue todo task", overdueDueAt);

        UUID inProgressTaskId =
                createTask(
                        tenant.tenantId(),
                        activeProjectId,
                        adminToken,
                        "Overdue in-progress task",
                        overdueDueAt);

        UUID blockedTaskId =
                createTask(
                        tenant.tenantId(),
                        activeProjectId,
                        adminToken,
                        "Future blocked task",
                        futureDueAt);

        UUID completedTaskId =
                createTask(
                        tenant.tenantId(),
                        activeProjectId,
                        adminToken,
                        "Completed overdue task",
                        overdueDueAt);

        UUID cancelledTaskId =
                createTask(
                        tenant.tenantId(),
                        activeProjectId,
                        adminToken,
                        "Cancelled overdue task",
                        overdueDueAt);

        updateTaskStatus(
                tenant.tenantId(), activeProjectId, inProgressTaskId, adminToken, "IN_PROGRESS");

        updateTaskStatus(tenant.tenantId(), activeProjectId, blockedTaskId, adminToken, "BLOCKED");

        updateTaskStatus(
                tenant.tenantId(), activeProjectId, completedTaskId, adminToken, "COMPLETED");

        cancelTask(tenant.tenantId(), activeProjectId, cancelledTaskId, adminToken);

        mockMvc.perform(
                        get("/api/tenant/dashboard/summary")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(1))
                .andExpect(jsonPath("$.data.activeUsers").value(1))
                .andExpect(jsonPath("$.data.totalProjects").value(5))
                .andExpect(jsonPath("$.data.planningProjects").value(1))
                .andExpect(jsonPath("$.data.activeProjects").value(1))
                .andExpect(jsonPath("$.data.onHoldProjects").value(1))
                .andExpect(jsonPath("$.data.completedProjects").value(1))
                .andExpect(jsonPath("$.data.archivedProjects").value(1))
                .andExpect(jsonPath("$.data.totalProjectMemberships").value(5))
                .andExpect(jsonPath("$.data.totalTasks").value(5))
                .andExpect(jsonPath("$.data.todoTasks").value(1))
                .andExpect(jsonPath("$.data.inProgressTasks").value(1))
                .andExpect(jsonPath("$.data.blockedTasks").value(1))
                .andExpect(jsonPath("$.data.completedTasks").value(1))
                .andExpect(jsonPath("$.data.cancelledTasks").value(1))
                .andExpect(jsonPath("$.data.overdueTasks").value(2))
                .andExpect(jsonPath("$.data.taskCompletionPercentage").value(25.0));
    }

    @Test
    void dashboardMetricsAreIsolatedBetweenTenants() throws Exception {

        TenantFixture tenantA = onboardUniqueTenant("dashboard-isolation-a");

        TenantFixture tenantB = onboardUniqueTenant("dashboard-isolation-b");

        String tenantAToken = login(tenantA.tenantId(), tenantA.adminEmail());

        String tenantBToken = login(tenantB.tenantId(), tenantB.adminEmail());

        UUID tenantBProjectId =
                createProject(tenantB.tenantId(), tenantBToken, "Tenant B Private Project");

        updateProjectStatus(tenantB.tenantId(), tenantBProjectId, tenantBToken, "ACTIVE");

        createTask(
                tenantB.tenantId(),
                tenantBProjectId,
                tenantBToken,
                "Tenant B Private Task",
                Instant.now().plus(7, ChronoUnit.DAYS));

        /*
         * Confirm that tenant B owns the created data.
         */
        mockMvc.perform(
                        get("/api/tenant/dashboard/summary")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantB.tenantId().toString()))
                .andExpect(jsonPath("$.data.totalUsers").value(1))
                .andExpect(jsonPath("$.data.totalProjects").value(1))
                .andExpect(jsonPath("$.data.activeProjects").value(1))
                .andExpect(jsonPath("$.data.totalProjectMemberships").value(1))
                .andExpect(jsonPath("$.data.totalTasks").value(1))
                .andExpect(jsonPath("$.data.todoTasks").value(1))
                .andExpect(jsonPath("$.data.overdueTasks").value(0))
                .andExpect(jsonPath("$.data.taskCompletionPercentage").value(0.0));

        /*
         * Tenant A's JWT must resolve only tenant A's data.
         */
        mockMvc.perform(
                        get("/api/tenant/dashboard/summary")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(tenantA.tenantId().toString()))
                .andExpect(jsonPath("$.data.tenantName").value(tenantA.tenantName()))
                .andExpect(jsonPath("$.data.tenantSlug").value(tenantA.tenantSlug()))
                .andExpect(jsonPath("$.data.totalUsers").value(1))
                .andExpect(jsonPath("$.data.activeUsers").value(1))
                .andExpect(jsonPath("$.data.totalProjects").value(0))
                .andExpect(jsonPath("$.data.activeProjects").value(0))
                .andExpect(jsonPath("$.data.totalProjectMemberships").value(0))
                .andExpect(jsonPath("$.data.totalTasks").value(0))
                .andExpect(jsonPath("$.data.todoTasks").value(0))
                .andExpect(jsonPath("$.data.overdueTasks").value(0))
                .andExpect(jsonPath("$.data.taskCompletionPercentage").value(0.0));
    }

    @Test
    void tenantManagerCanAccessDashboardButTenantUserCannot() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("dashboard-role-access");

        String adminToken = login(tenant.tenantId(), tenant.adminEmail());

        String suffix = uniqueSuffix();

        String managerEmail = "manager." + suffix + "@example.test";

        String userEmail = "user." + suffix + "@example.test";

        createAndAcceptInvitedUser(
                tenant.tenantId(), adminToken, "Dashboard Manager", managerEmail, "TENANT_MANAGER");

        createAndAcceptInvitedUser(
                tenant.tenantId(), adminToken, "Dashboard User", userEmail, "TENANT_USER");

        String managerToken = login(tenant.tenantId(), managerEmail, INVITED_USER_PASSWORD);

        String userToken = login(tenant.tenantId(), userEmail, INVITED_USER_PASSWORD);

        /*
         * TENANT_MANAGER is authorized to view the complete
         * tenant-level dashboard.
         */
        mockMvc.perform(
                        get("/api/tenant/dashboard/summary")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId").value(tenant.tenantId().toString()))
                .andExpect(jsonPath("$.data.totalUsers").value(3))
                .andExpect(jsonPath("$.data.activeUsers").value(3))
                .andExpect(jsonPath("$.data.inactiveUsers").value(0))
                .andExpect(jsonPath("$.data.suspendedUsers").value(0))
                .andExpect(jsonPath("$.data.totalProjects").value(0))
                .andExpect(jsonPath("$.data.totalProjectMemberships").value(0))
                .andExpect(jsonPath("$.data.totalTasks").value(0))
                .andExpect(jsonPath("$.data.taskCompletionPercentage").value(0.0));

        /*
         * TENANT_USER is authenticated but lacks permission
         * to view tenant-wide dashboard information.
         */
        mockMvc.perform(
                        get("/api/tenant/dashboard/summary")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/tenant/dashboard/summary"));
    }

    private TenantFixture onboardUniqueTenant(String prefix) throws Exception {

        String suffix = uniqueSuffix();
        String tenantName = prefix + " Tenant";
        String tenantSlug = prefix + "-" + suffix;
        String adminEmail = "admin." + suffix + "@example.test";

        String requestBody =
                """
                {
                  "tenantName": "%s",
                  "tenantSlug": "%s",
                  "adminFullName": "Dashboard Test Administrator",
                  "adminEmail": "%s",
                  "adminPassword": "%s"
                }
                """
                        .formatted(tenantName, tenantSlug, adminEmail, ADMIN_PASSWORD);

        MvcResult result =
                mockMvc.perform(
                                post("/api/onboarding/tenants")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return new TenantFixture(
                UUID.fromString(response.at("/data/tenant/id").asString()),
                tenantName,
                tenantSlug,
                adminEmail);
    }

    private String login(UUID tenantId, String email) throws Exception {

        return login(tenantId, email, ADMIN_PASSWORD);
    }

    private String login(UUID tenantId, String email, String password) throws Exception {

        String requestBody =
                """
            {
              "email": "%s",
              "password": "%s"
            }
            """
                        .formatted(email, password);

        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/auth/login", tenantId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.email").value(email))
                        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return response.at("/data/accessToken").asString();
    }

    private UUID createProject(UUID tenantId, String accessToken, String name) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/projects", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                    {
                                      "name": "%s",
                                      "description": "Dashboard integration project."
                                    }
                                    """
                                                        .formatted(name)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("PLANNING"))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return UUID.fromString(response.at("/data/id").asString());
    }

    private void updateProjectStatus(
            UUID tenantId, UUID projectId, String accessToken, String projectStatus)
            throws Exception {

        mockMvc.perform(
                        patch(
                                        "/api/tenants/{tenantId}" + "/projects/{projectId}/status",
                                        tenantId,
                                        projectId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                    {
                                      "status": "%s"
                                    }
                                    """
                                                .formatted(projectStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(projectStatus));
    }

    private void archiveProject(UUID tenantId, UUID projectId, String accessToken)
            throws Exception {

        mockMvc.perform(
                        delete(
                                        "/api/tenants/{tenantId}" + "/projects/{projectId}",
                                        tenantId,
                                        projectId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    private UUID createTask(
            UUID tenantId, UUID projectId, String accessToken, String title, Instant dueAt)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post(
                                                "/api/tenants/{tenantId}"
                                                        + "/projects/{projectId}/tasks",
                                                tenantId,
                                                projectId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                    {
                                      "title": "%s",
                                      "description": "Dashboard integration task.",
                                      "priority": "MEDIUM",
                                      "dueAt": "%s",
                                      "assigneeUserId": null
                                    }
                                    """
                                                        .formatted(title, dueAt)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("TODO"))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return UUID.fromString(response.at("/data/id").asString());
    }

    private void updateTaskStatus(
            UUID tenantId, UUID projectId, UUID taskId, String accessToken, String taskStatus)
            throws Exception {

        mockMvc.perform(
                        patch(
                                        "/api/tenants/{tenantId}"
                                                + "/projects/{projectId}"
                                                + "/tasks/{taskId}/status",
                                        tenantId,
                                        projectId,
                                        taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                    {
                                      "status": "%s"
                                    }
                                    """
                                                .formatted(taskStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(taskStatus));
    }

    private void cancelTask(UUID tenantId, UUID projectId, UUID taskId, String accessToken)
            throws Exception {

        mockMvc.perform(
                        delete(
                                        "/api/tenants/{tenantId}"
                                                + "/projects/{projectId}"
                                                + "/tasks/{taskId}",
                                        tenantId,
                                        projectId,
                                        taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private void createAndAcceptInvitedUser(
            UUID tenantId, String adminToken, String fullName, String email, String role)
            throws Exception {

        MvcResult invitationResult =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/user-invitations", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                    {
                                      "fullName": "%s",
                                      "email": "%s",
                                      "role": "%s"
                                    }
                                    """
                                                        .formatted(fullName, email, role)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.email").value(email))
                        .andExpect(jsonPath("$.data.role").value(role))
                        .andExpect(jsonPath("$.data.status").value("PENDING"))
                        .andExpect(jsonPath("$.data.devInvitationToken").isNotEmpty())
                        .andReturn();

        JsonNode invitationResponse =
                jsonMapper.readTree(invitationResult.getResponse().getContentAsString());

        String invitationToken = invitationResponse.at("/data/devInvitationToken").asString();

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
                                                        INVITED_USER_PASSWORD,
                                                        INVITED_USER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value(role))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record TenantFixture(
            UUID tenantId, String tenantName, String tenantSlug, String adminEmail) {}
}
