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
class CriticalTenantJourneyIntegrationTest {

    private static final String ADMIN_PASSWORD = "TenantAdmin@123";
    private static final String MEMBER_PASSWORD = "ProjectMember@123";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper jsonMapper;

    @Test
    void tenantJourneyConnectsMembershipTasksCollaborationNotificationsAndSessionRevocation()
            throws Exception {
        TenantFixture tenant = onboardUniqueTenant();
        SessionTokens adminSession = login(tenant.tenantId(), tenant.adminEmail(), ADMIN_PASSWORD);

        UserFixture member =
                inviteAndAcceptUser(
                        tenant.tenantId(),
                        adminSession.accessToken(),
                        "Critical Journey Member",
                        "journey.member." + uniqueSuffix() + "@example.test");

        UUID projectId =
                createProject(
                        tenant.tenantId(),
                        adminSession.accessToken(),
                        "Critical Journey Project");
        addProjectMember(
                tenant.tenantId(),
                projectId,
                adminSession.accessToken(),
                member.userId(),
                "MEMBER");

        UUID taskId =
                createTask(
                        tenant.tenantId(),
                        projectId,
                        adminSession.accessToken(),
                        member.userId());

        SessionTokens memberSession = login(tenant.tenantId(), member.email(), MEMBER_PASSWORD);

        UUID commentId =
                createMentionedComment(
                        tenant.tenantId(),
                        projectId,
                        taskId,
                        adminSession.accessToken(),
                        member.userId());

        String commentTarget =
                "/projects/" + projectId + "?task=" + taskId + "&comment=" + commentId;

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/notifications", tenant.tenantId())
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].type").value("TASK_COMMENT_MENTIONED"))
                .andExpect(jsonPath("$.data.content[0].targetUrl").value(commentTarget))
                .andExpect(jsonPath("$.data.content[1].type").value("TASK_ASSIGNED"))
                .andExpect(
                        jsonPath("$.data.content[2].type").value("PROJECT_MEMBERSHIP_CHANGED"));

        UUID replyId =
                createReply(
                        tenant.tenantId(),
                        projectId,
                        taskId,
                        commentId,
                        memberSession.accessToken());

        String replyTarget = commentTarget + "&reply=" + replyId;

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/notifications", tenant.tenantId())
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("TASK_COMMENT_REPLIED"))
                .andExpect(jsonPath("$.data.content[0].targetUrl").value(replyTarget));

        updateTaskStatus(
                        tenant.tenantId(),
                        projectId,
                        taskId,
                        adminSession.accessToken(),
                        "IN_PROGRESS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        MvcResult memberNotifications =
                mockMvc.perform(
                                get("/api/tenants/{tenantId}/notifications", tenant.tenantId())
                                        .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + memberSession.accessToken()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalElements").value(4))
                        .andExpect(jsonPath("$.data.content[0].type").value("TASK_STATUS_CHANGED"))
                        .andExpect(
                                jsonPath("$.data.content[0].targetUrl")
                                        .value("/projects/" + projectId + "?task=" + taskId))
                        .andReturn();

        UUID statusNotificationId =
                UUID.fromString(
                        jsonMapper
                                .readTree(memberNotifications.getResponse().getContentAsString())
                                .at("/data/content/0/id")
                                .asString());

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/notifications/unread-count", tenant.tenantId())
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(4));

        mockMvc.perform(
                        patch(
                                        "/api/tenants/{tenantId}/notifications/{notificationId}/read",
                                        tenant.tenantId(),
                                        statusNotificationId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        updateTaskStatus(
                        tenant.tenantId(),
                        projectId,
                        taskId,
                        memberSession.accessToken(),
                        "COMPLETED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        mockMvc.perform(
                        get("/api/tenants/{tenantId}/notifications/unread-count", tenant.tenantId())
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));

        mockMvc.perform(
                        patch("/api/tenants/{tenantId}/notifications/read-all", tenant.tenantId())
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedReadCount").value(3));

        mockMvc.perform(
                        post("/api/auth/logout-all")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.message")
                                .value("Logged out from all devices successfully"));

        assertRefreshRejected(memberSession.refreshToken());
        assertAccessTokenRejected(memberSession.accessToken());
    }

    private TenantFixture onboardUniqueTenant() throws Exception {
        String suffix = uniqueSuffix();
        String slug = "critical-journey-" + suffix;
        String adminEmail = "journey.admin." + suffix + "@example.test";

        MvcResult result =
                mockMvc.perform(
                                post("/api/onboarding/tenants")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "tenantName": "Critical Journey Tenant",
                                                  "tenantSlug": "%s",
                                                  "adminFullName": "Critical Journey Admin",
                                                  "adminEmail": "%s",
                                                  "adminPassword": "%s"
                                                }
                                                """
                                                        .formatted(
                                                                slug,
                                                                adminEmail,
                                                                ADMIN_PASSWORD)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.tenant.slug").value(slug))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return new TenantFixture(
                UUID.fromString(response.at("/data/tenant/id").asString()),
                UUID.fromString(response.at("/data/adminUser/id").asString()),
                adminEmail);
    }

    private SessionTokens login(UUID tenantId, String email, String password) throws Exception {
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
                        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                        .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return new SessionTokens(
                response.at("/data/accessToken").asString(),
                response.at("/data/refreshToken").asString());
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

        String invitationToken =
                jsonMapper
                        .readTree(invitationResult.getResponse().getContentAsString())
                        .at("/data/devInvitationToken")
                        .asString();

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

        JsonNode response = jsonMapper.readTree(acceptanceResult.getResponse().getContentAsString());
        return new UserFixture(UUID.fromString(response.at("/data/user/id").asString()), email);
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
                                                  "description": "Cross-module critical journey project."
                                                }
                                                """
                                                        .formatted(name)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.at("/data/id").asString());
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId.toString()));
    }

    private UUID createTask(
            UUID tenantId, UUID projectId, String accessToken, UUID assigneeUserId)
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
                                                  "title": "Ship the critical journey",
                                                  "description": "Exercise the cross-module tenant workflow.",
                                                  "priority": "HIGH",
                                                  "dueAt": null,
                                                  "assigneeUserId": "%s"
                                                }
                                                """
                                                        .formatted(assigneeUserId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("TODO"))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.at("/data/id").asString());
    }

    private UUID createMentionedComment(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            String accessToken,
            UUID mentionedUserId)
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
                                        .content(
                                                """
                                                {
                                                  "body": "Please verify the critical journey before billing.",
                                                  "mentionedUserIds": ["%s"]
                                                }
                                                """
                                                        .formatted(mentionedUserId)))
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.data.mentions[0].userId")
                                        .value(mentionedUserId.toString()))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.at("/data/id").asString());
    }

    private UUID createReply(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID commentId,
            String accessToken)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                                "/api/tenants/{tenantId}/projects/{projectId}"
                                                        + "/tasks/{taskId}/comments/{commentId}/replies",
                                                tenantId,
                                                projectId,
                                                taskId,
                                                commentId)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "body": "Verified. The critical journey is connected.",
                                                  "mentionedUserIds": []
                                                }
                                                """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.parentCommentId").value(commentId.toString()))
                        .andReturn();

        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.at("/data/id").asString());
    }

    private org.springframework.test.web.servlet.ResultActions updateTaskStatus(
            UUID tenantId, UUID projectId, UUID taskId, String accessToken, String status)
            throws Exception {
        return mockMvc.perform(
                patch(
                                "/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}/status",
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
                                        .formatted(status)));
    }

    private void assertRefreshRejected(String refreshToken) throws Exception {
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """
                                                .formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
    }

    private void assertAccessTokenRejected(String accessToken) throws Exception {
        mockMvc.perform(
                        get("/api/auth/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record TenantFixture(UUID tenantId, UUID adminUserId, String adminEmail) {}

    private record UserFixture(UUID userId, String email) {}

    private record SessionTokens(String accessToken, String refreshToken) {}
}
