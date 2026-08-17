package com.chacha.multitenantsaas.integration;

import static org.hamcrest.Matchers.nullValue;
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
class TaskCollaborationIntegrationTest {

    private static final String ADMIN_PASSWORD = "TenantAdmin@123";
    private static final String MEMBER_PASSWORD = "ProjectMember@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Test
    void projectMembersCanCommentMentionEditDeleteAndReadActivity() throws Exception {
        TenantFixture tenant = onboardUniqueTenant("task-collaboration");
        String adminToken = login(tenant.tenantId(), tenant.adminEmail(), ADMIN_PASSWORD);
        UserFixture member =
                inviteAndAcceptUser(
                        tenant.tenantId(),
                        adminToken,
                        "Collaboration Member",
                        "collab." + uniqueSuffix() + "@example.test");

        UUID projectId = createProject(tenant.tenantId(), adminToken, "Collaboration Project");
        addProjectMember(tenant.tenantId(), projectId, adminToken, member.userId(), "MEMBER");
        UUID taskId =
                createTask(
                        tenant.tenantId(),
                        projectId,
                        adminToken,
                        "Review collaboration flow",
                        member.userId());
        String memberToken = login(tenant.tenantId(), member.email(), MEMBER_PASSWORD);

        MvcResult createResult =
                mockMvc.perform(
                                post(
                                                "/api/tenants/{tenantId}/projects/{projectId}"
                                                        + "/tasks/{taskId}/comments",
                                                tenant.tenantId(),
                                                projectId,
                                                taskId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "body": "  Please review this with the administrator.  ",
                                                  "mentionedUserIds": ["%s"]
                                                }
                                                """
                                                        .formatted(tenant.adminUserId())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.authorUserId").value(member.userId().toString()))
                        .andExpect(
                                jsonPath("$.data.body")
                                        .value("Please review this with the administrator."))
                        .andExpect(
                                jsonPath("$.data.mentions[0].userId")
                                        .value(tenant.adminUserId().toString()))
                        .andReturn();

        JsonNode created = jsonMapper.readTree(createResult.getResponse().getContentAsString());
        UUID commentId = UUID.fromString(created.at("/data/id").asString());

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/comments",
                                        tenant.tenantId(),
                                        projectId,
                                        taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(commentId.toString()));

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/activity",
                                        tenant.tenantId(),
                                        projectId,
                                        taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("COMMENT_ADDED"))
                .andExpect(jsonPath("$.data.content[0].actorUserId").value(member.userId().toString()));

        mockMvc.perform(
                        put(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/comments/{commentId}",
                                        tenant.tenantId(),
                                        projectId,
                                        taskId,
                                        commentId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "body": "Updated collaboration context.",
                                          "mentionedUserIds": []
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("Updated collaboration context."))
                .andExpect(jsonPath("$.data.editedAt").isNotEmpty());

        mockMvc.perform(
                        delete(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/comments/{commentId}",
                                        tenant.tenantId(),
                                        projectId,
                                        taskId,
                                        commentId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andExpect(jsonPath("$.data.body").value(nullValue()))
                .andExpect(jsonPath("$.data.deletedAt").isNotEmpty());
    }

    @Test
    void commentOwnershipMentionScopeAndTenantIsolationAreEnforced() throws Exception {
        TenantFixture tenantA = onboardUniqueTenant("task-collab-a");
        String adminAToken = login(tenantA.tenantId(), tenantA.adminEmail(), ADMIN_PASSWORD);
        UserFixture projectMember =
                inviteAndAcceptUser(
                        tenantA.tenantId(),
                        adminAToken,
                        "Project Collaborator",
                        "member." + uniqueSuffix() + "@example.test");
        UserFixture outsider =
                inviteAndAcceptUser(
                        tenantA.tenantId(),
                        adminAToken,
                        "Workspace Outsider",
                        "outsider." + uniqueSuffix() + "@example.test");

        UUID projectId = createProject(tenantA.tenantId(), adminAToken, "Scoped Collaboration");
        addProjectMember(
                tenantA.tenantId(), projectId, adminAToken, projectMember.userId(), "MEMBER");
        UUID taskId =
                createTask(
                        tenantA.tenantId(),
                        projectId,
                        adminAToken,
                        "Protect comment ownership",
                        projectMember.userId());
        String projectMemberToken =
                login(tenantA.tenantId(), projectMember.email(), MEMBER_PASSWORD);

        UUID commentId =
                createComment(
                        tenantA.tenantId(),
                        projectId,
                        taskId,
                        adminAToken,
                        "Administrator-owned comment.");

        String commentPath =
                "/api/tenants/"
                        + tenantA.tenantId()
                        + "/projects/"
                        + projectId
                        + "/tasks/"
                        + taskId
                        + "/comments/"
                        + commentId;

        mockMvc.perform(
                        put(commentPath)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + projectMemberToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "body": "Attempted takeover.",
                                          "mentionedUserIds": []
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        mockMvc.perform(
                        post(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/comments",
                                        tenantA.tenantId(),
                                        projectId,
                                        taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "body": "Invalid project mention.",
                                          "mentionedUserIds": ["%s"]
                                        }
                                        """
                                                .formatted(outsider.userId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Mentioned users must be members of the current project"));

        TenantFixture tenantB = onboardUniqueTenant("task-collab-b");
        String adminBToken = login(tenantB.tenantId(), tenantB.adminEmail(), ADMIN_PASSWORD);

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/comments",
                                        tenantA.tenantId(),
                                        projectId,
                                        taskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void cancelledTasksAndArchivedProjectsKeepCollaborationReadOnly() throws Exception {
        TenantFixture tenant = onboardUniqueTenant("task-collab-readonly");
        String adminToken = login(tenant.tenantId(), tenant.adminEmail(), ADMIN_PASSWORD);
        UUID projectId = createProject(tenant.tenantId(), adminToken, "Read-only Collaboration");

        UUID cancelledTaskId =
                createTask(
                        tenant.tenantId(),
                        projectId,
                        adminToken,
                        "Cancelled collaboration",
                        tenant.adminUserId());
        UUID archivedTaskId =
                createTask(
                        tenant.tenantId(),
                        projectId,
                        adminToken,
                        "Archived collaboration",
                        tenant.adminUserId());

        mockMvc.perform(
                        delete(
                                        "/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}",
                                        tenant.tenantId(),
                                        projectId,
                                        cancelledTaskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/comments",
                                        tenant.tenantId(),
                                        projectId,
                                        cancelledTaskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(commentRequest("No new discussion.")))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Cancelled task collaboration is read-only"));

        mockMvc.perform(
                        delete(
                                        "/api/tenants/{tenantId}/projects/{projectId}",
                                        tenant.tenantId(),
                                        projectId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(
                        post(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/comments",
                                        tenant.tenantId(),
                                        projectId,
                                        archivedTaskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(commentRequest("Archived discussion.")))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Archived project collaboration is read-only"));

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/projects/{projectId}"
                                                + "/tasks/{taskId}/activity",
                                        tenant.tenantId(),
                                        projectId,
                                        archivedTaskId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("TASK_CREATED"));
    }

    private UUID createComment(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            String accessToken,
            String body)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                                "/api/tenants/{tenantId}/projects/{projectId}"
                                                        + "/tasks/{taskId}/comments",
                                                tenantId,
                                                projectId,
                                                taskId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(commentRequest(body)))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.at("/data/id").asString());
    }

    private String commentRequest(String body) {
        return """
                {
                  "body": "%s",
                  "mentionedUserIds": []
                }
                """
                .formatted(body);
    }

    private TenantFixture onboardUniqueTenant(String prefix) throws Exception {
        String suffix = uniqueSuffix();
        String slug = prefix + "-" + suffix;
        String adminEmail = "admin." + suffix + "@example.test";

        MvcResult result =
                mockMvc.perform(
                                post("/api/onboarding/tenants")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "tenantName": "%s",
                                                  "tenantSlug": "%s",
                                                  "adminFullName": "Collaboration Administrator",
                                                  "adminEmail": "%s",
                                                  "adminPassword": "%s"
                                                }
                                                """
                                                        .formatted(
                                                                prefix + " Tenant",
                                                                slug,
                                                                adminEmail,
                                                                ADMIN_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return new TenantFixture(
                UUID.fromString(response.at("/data/tenant/id").asString()),
                UUID.fromString(response.at("/data/adminUser/id").asString()),
                adminEmail);
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
                                                  "description": "Task collaboration integration project."
                                                }
                                                """
                                                        .formatted(name)))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.at("/data/id").asString());
    }

    private UUID createTask(
            UUID tenantId,
            UUID projectId,
            String accessToken,
            String title,
            UUID assigneeUserId)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                                "/api/tenants/{tenantId}/projects/{projectId}/tasks",
                                                tenantId,
                                                projectId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "title": "%s",
                                                  "description": "Collaboration test task.",
                                                  "priority": "MEDIUM",
                                                  "dueAt": "2026-08-25T12:00:00Z",
                                                  "assigneeUserId": "%s"
                                                }
                                                """
                                                        .formatted(title, assigneeUserId)))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.at("/data/id").asString());
    }

    private UserFixture inviteAndAcceptUser(
            UUID tenantId, String adminToken, String fullName, String email) throws Exception {
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
                                                  "role": "TENANT_USER"
                                                }
                                                """
                                                        .formatted(fullName, email)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode invitationResponse =
                jsonMapper.readTree(invitationResult.getResponse().getContentAsString());
        String invitationToken = invitationResponse.at("/data/devInvitationToken").asString();

        MvcResult acceptanceResult =
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
                                                                MEMBER_PASSWORD,
                                                                MEMBER_PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode acceptanceResponse =
                jsonMapper.readTree(acceptanceResult.getResponse().getContentAsString());
        return new UserFixture(
                UUID.fromString(acceptanceResponse.at("/data/user/id").asString()), email);
    }

    private void addProjectMember(
            UUID tenantId, UUID projectId, String accessToken, UUID userId, String role)
            throws Exception {
        mockMvc.perform(
                        post(
                                        "/api/tenants/{tenantId}/projects/{projectId}/members",
                                        tenantId,
                                        projectId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "userId": "%s",
                                          "role": "%s"
                                        }
                                        """
                                                .formatted(userId, role)))
                .andExpect(status().isOk());
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record TenantFixture(UUID tenantId, UUID adminUserId, String adminEmail) {}

    private record UserFixture(UUID userId, String email) {}
}
