package com.chacha.multitenantsaas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
class ProjectIntegrationTest {

    private static final String ADMIN_PASSWORD = "TenantAdmin@123";

    private static final String MEMBER_PASSWORD = "TenantMember@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Test
    void tenantAdminCanManageCompleteProjectLifecycle() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("project-admin");

        String adminToken = login(tenant.tenantId(), tenant.adminEmail(), ADMIN_PASSWORD);

        ProjectFixture project =
                createProject(
                        tenant.tenantId(),
                        adminToken,
                        "Customer Portal",
                        "Initial customer portal implementation.");

        /*
         * Read the newly created project.
         */
        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(project.projectId().toString()))
                .andExpect(jsonPath("$.data.tenantId").value(tenant.tenantId().toString()))
                .andExpect(jsonPath("$.data.name").value("Customer Portal"))
                .andExpect(jsonPath("$.data.status").value("PLANNING"))
                .andExpect(jsonPath("$.data.createdByUserEmail").value(tenant.adminEmail()));

        /*
         * Update the project.
         */
        String updateRequest =
                """
                {
                  "name": "Customer Management Portal",
                  "description": "Updated project description."
                }
                """;

        mockMvc.perform(
                        put(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Customer Management Portal"))
                .andExpect(jsonPath("$.data.description").value("Updated project description."))
                .andExpect(jsonPath("$.data.status").value("PLANNING"));

        /*
         * Change status.
         */
        mockMvc.perform(
                        patch(
                                        "/api/tenants/{tenantId}" + "/projects/{projectId}/status",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "status": "ACTIVE"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        /*
         * Archive the project.
         */
        mockMvc.perform(
                        delete(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    @Test
    void projectListSupportsSearchStatusFilterAndPagination() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("project-filter");

        String adminToken = login(tenant.tenantId(), tenant.adminEmail(), ADMIN_PASSWORD);

        ProjectFixture customerProject =
                createProject(
                        tenant.tenantId(),
                        adminToken,
                        "Customer Portal",
                        "Portal for customer operations.");

        createProject(
                tenant.tenantId(), adminToken, "Internal Analytics", "Internal reporting module.");

        mockMvc.perform(
                        patch(
                                        "/api/tenants/{tenantId}" + "/projects/{projectId}/status",
                                        tenant.tenantId(),
                                        customerProject.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "status": "ACTIVE"
                                        }
                                        """))
                .andExpect(status().isOk());

        /*
         * Search by project name.
         */
        mockMvc.perform(
                        get("/api/tenants/{tenantId}/projects", tenant.tenantId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .param("search", "customer")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "name")
                                .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Customer Portal"));

        /*
         * Filter by project status.
         */
        mockMvc.perform(
                        get("/api/tenants/{tenantId}/projects", tenant.tenantId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .param("status", "ACTIVE")
                                .param("page", "0")
                                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(
                        jsonPath("$.data.content[0].id")
                                .value(customerProject.projectId().toString()))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    @Test
    void tenantManagerCanCreateAndUpdateProjects() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("project-manager");

        String adminToken = login(tenant.tenantId(), tenant.adminEmail(), ADMIN_PASSWORD);

        UserFixture manager =
                inviteAndAcceptUser(
                        tenant.tenantId(),
                        adminToken,
                        "Project Manager",
                        "manager." + uniqueSuffix() + "@example.test",
                        "TENANT_MANAGER");

        String managerToken = login(tenant.tenantId(), manager.email(), MEMBER_PASSWORD);

        ProjectFixture project =
                createProject(
                        tenant.tenantId(),
                        managerToken,
                        "Manager Project",
                        "Created by a tenant manager.");

        String updateRequest =
                """
                {
                  "name": "Updated Manager Project",
                  "description": "Updated by the tenant manager."
                }
                """;

        mockMvc.perform(
                        put(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Manager Project"))
                .andExpect(jsonPath("$.data.createdByUserEmail").value(manager.email()));
    }

    @Test
    void tenantUserCanReadProjectsButCannotModifyThem() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("project-user");

        String adminToken = login(tenant.tenantId(), tenant.adminEmail(), ADMIN_PASSWORD);

        ProjectFixture project =
                createProject(
                        tenant.tenantId(),
                        adminToken,
                        "Read Only Project",
                        "Visible to all tenant users.");

        UserFixture tenantUser =
                inviteAndAcceptUser(
                        tenant.tenantId(),
                        adminToken,
                        "Read Only User",
                        "user." + uniqueSuffix() + "@example.test",
                        "TENANT_USER");

        String userToken = login(tenant.tenantId(), tenantUser.email(), MEMBER_PASSWORD);

        /*
         * Tenant users can list projects.
         */
        mockMvc.perform(
                        get("/api/tenants/{tenantId}/projects", tenant.tenantId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        /*
         * Tenant users can read a specific project.
         */
        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Read Only Project"));

        /*
         * Tenant users cannot create projects.
         */
        String createRequest =
                """
                {
                  "name": "Forbidden Project",
                  "description": "Should not be created."
                }
                """;

        String collectionPath = "/api/tenants/" + tenant.tenantId() + "/projects";

        mockMvc.perform(
                        post("/api/tenants/{tenantId}/projects", tenant.tenantId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value(collectionPath));

        /*
         * Tenant users cannot update projects.
         */
        String projectPath = collectionPath + "/" + project.projectId();

        mockMvc.perform(
                        put(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Forbidden Update",
                                          "description": "Should not update."
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value(projectPath));
    }

    @Test
    void tenantCannotAccessAnotherTenantsProjects() throws Exception {

        TenantFixture tenantA = onboardUniqueTenant("project-tenant-a");

        TenantFixture tenantB = onboardUniqueTenant("project-tenant-b");

        String tenantAToken = login(tenantA.tenantId(), tenantA.adminEmail(), ADMIN_PASSWORD);

        String tenantBToken = login(tenantB.tenantId(), tenantB.adminEmail(), ADMIN_PASSWORD);

        ProjectFixture tenantBProject =
                createProject(
                        tenantB.tenantId(),
                        tenantBToken,
                        "Tenant B Project",
                        "Private tenant B project.");

        String expectedCollectionPath = "/api/tenants/" + tenantB.tenantId() + "/projects";

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/projects", tenantB.tenantId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value(expectedCollectionPath));

        String expectedProjectPath = expectedCollectionPath + "/" + tenantBProject.projectId();

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenantB.tenantId(),
                                        tenantBProject.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value(expectedProjectPath));
    }

    @Test
    void archivedProjectCannotBeModifiedOrArchivedAgain() throws Exception {

        TenantFixture tenant = onboardUniqueTenant("project-archive");

        String adminToken = login(tenant.tenantId(), tenant.adminEmail(), ADMIN_PASSWORD);

        ProjectFixture project =
                createProject(
                        tenant.tenantId(), adminToken, "Archived Project", "Project to archive.");

        mockMvc.perform(
                        delete(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        String projectPath =
                "/api/tenants/" + tenant.tenantId() + "/projects/" + project.projectId();

        mockMvc.perform(
                        put(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Modified Archive",
                                          "description": "Not allowed."
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(projectPath));

        mockMvc.perform(
                        delete(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        project.projectId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));
    }

    private TenantFixture onboardUniqueTenant(String prefix) throws Exception {

        String suffix = uniqueSuffix();
        String slug = prefix + "-" + suffix;
        String adminEmail = "admin." + suffix + "@example.test";

        String requestBody =
                """
                {
                  "tenantName": "%s",
                  "tenantSlug": "%s",
                  "adminFullName": "Project Test Administrator",
                  "adminEmail": "%s",
                  "adminPassword": "%s"
                }
                """
                        .formatted(prefix + " Tenant", slug, adminEmail, ADMIN_PASSWORD);

        MvcResult result =
                mockMvc.perform(
                                post("/api/onboarding/tenants")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return new TenantFixture(
                UUID.fromString(response.at("/data/tenant/id").asString()), adminEmail);
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
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return response.at("/data/accessToken").asString();
    }

    private ProjectFixture createProject(
            UUID tenantId, String accessToken, String name, String description) throws Exception {

        String requestBody =
                """
                {
                  "name": "%s",
                  "description": "%s"
                }
                """
                        .formatted(name, description);

        MvcResult result =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/projects", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.name").value(name))
                        .andExpect(jsonPath("$.data.status").value("PLANNING"))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());

        return new ProjectFixture(UUID.fromString(response.at("/data/id").asString()));
    }

    private UserFixture inviteAndAcceptUser(
            UUID tenantId, String adminToken, String fullName, String email, String role)
            throws Exception {

        String invitationRequest =
                """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "role": "%s"
                }
                """
                        .formatted(fullName, email, role);

        MvcResult invitationResult =
                mockMvc.perform(
                                post("/api/tenants/{tenantId}/user-invitations", tenantId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(invitationRequest))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode invitationResponse =
                jsonMapper.readTree(invitationResult.getResponse().getContentAsString());

        String invitationToken = invitationResponse.at("/data/devInvitationToken").asString();

        String acceptRequest =
                """
                {
                  "invitationToken": "%s",
                  "newPassword": "%s",
                  "confirmPassword": "%s"
                }
                """
                        .formatted(invitationToken, MEMBER_PASSWORD, MEMBER_PASSWORD);

        mockMvc.perform(
                        post("/api/user-invitations/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value(role));

        return new UserFixture(email);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record TenantFixture(UUID tenantId, String adminEmail) {}

    private record ProjectFixture(UUID projectId) {}

    private record UserFixture(String email) {}
}
