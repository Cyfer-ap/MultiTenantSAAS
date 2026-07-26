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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectMemberIntegrationTest {

    private static final String ADMIN_PASSWORD =
            "TenantAdmin@123";

    private static final String MEMBER_PASSWORD =
            "ProjectMember@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void projectCreatorIsAutomaticallyAssignedAsProjectLead()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("member-auto-lead");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Automatic Lead Project"
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.data.content[0].userId")
                        .value(tenant.adminUserId().toString()))
                .andExpect(jsonPath("$.data.content[0].email")
                        .value(tenant.adminEmail()))
                .andExpect(jsonPath("$.data.content[0].projectRole")
                        .value("PROJECT_LEAD"))
                .andExpect(jsonPath(
                        "$.data.content[0].assignedByUserId"
                ).value(tenant.adminUserId().toString()))
                .andExpect(jsonPath(
                        "$.data.content[0].assignedByUserEmail"
                ).value(tenant.adminEmail()));
    }

    @Test
    void tenantAdminCanAddSearchPromoteAndRemoveMember()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("member-lifecycle");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Membership Lifecycle Project"
        );

        UserFixture member = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Project Member",
                "member." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        addProjectMember(
                tenant.tenantId(),
                projectId,
                adminToken,
                member.userId(),
                "MEMBER"
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .param("search", member.email())
                                .param("role", "MEMBER")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.data.content[0].userId")
                        .value(member.userId().toString()))
                .andExpect(jsonPath("$.data.content[0].email")
                        .value(member.email()))
                .andExpect(jsonPath("$.data.content[0].projectRole")
                        .value("MEMBER"));

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/members/{userId}/role",
                                tenant.tenantId(),
                                projectId,
                                member.userId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "PROJECT_LEAD"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectRole")
                        .value("PROJECT_LEAD"));

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/members/{userId}",
                                tenant.tenantId(),
                                projectId,
                                member.userId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectRole")
                        .value("PROJECT_LEAD"));

        /*
         * The original administrator remains a project lead,
         * so removing the promoted member is allowed.
         */
        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/members/{userId}",
                                tenant.tenantId(),
                                projectId,
                                member.userId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId")
                        .value(member.userId().toString()));

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/members/{userId}",
                                tenant.tenantId(),
                                projectId,
                                member.userId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode")
                        .value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void duplicateProjectMembershipIsRejected()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("member-duplicate");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Duplicate Membership Project"
        );

        UserFixture member = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Duplicate Member",
                "duplicate." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        addProjectMember(
                tenant.tenantId(),
                projectId,
                adminToken,
                member.userId(),
                "MEMBER"
        );

        String requestBody = projectMemberRequest(
                member.userId(),
                "MEMBER"
        );

        String expectedPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/members";

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode")
                        .value("RESOURCE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path")
                        .value(expectedPath));
    }

    @Test
    void lastProjectLeadCannotBeDemotedOrRemoved()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("member-last-lead");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Last Lead Protection Project"
        );

        String memberPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/members/"
                        + tenant.adminUserId();

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/members/{userId}/role",
                                tenant.tenantId(),
                                projectId,
                                tenant.adminUserId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "role": "MEMBER"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Project must have at least one project lead"
                        ))
                .andExpect(jsonPath("$.path")
                        .value(memberPath + "/role"));

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/members/{userId}",
                                tenant.tenantId(),
                                projectId,
                                tenant.adminUserId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Project must have at least one project lead"
                        ))
                .andExpect(jsonPath("$.path")
                        .value(memberPath));
    }

    @Test
    void tenantManagerCanManageProjectMembers()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("member-manager");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UserFixture manager = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Tenant Project Manager",
                "manager." + uniqueSuffix() + "@example.test",
                "TENANT_MANAGER"
        );

        UserFixture member = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Managed Project User",
                "managed." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        String managerToken = login(
                tenant.tenantId(),
                manager.email(),
                MEMBER_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                managerToken,
                "Manager Membership Project"
        );

        /*
         * The manager created the project and therefore
         * becomes its initial project lead.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + managerToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId")
                        .value(manager.userId().toString()))
                .andExpect(jsonPath("$.data.content[0].projectRole")
                        .value("PROJECT_LEAD"));

        addProjectMember(
                tenant.tenantId(),
                projectId,
                managerToken,
                member.userId(),
                "MEMBER"
        );

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/members/{userId}",
                                tenant.tenantId(),
                                projectId,
                                member.userId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + managerToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void tenantUserCanReadMembersButCannotManageThem()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("member-read-only");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UserFixture tenantUser = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Read Only Tenant User",
                "readonly." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        String userToken = login(
                tenant.tenantId(),
                tenantUser.email(),
                MEMBER_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Read Only Membership Project"
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1));

        String collectionPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/members";

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + userToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        projectMemberRequest(
                                                tenantUser.userId(),
                                                "MEMBER"
                                        )
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path")
                        .value(collectionPath));

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/members/{userId}",
                                tenant.tenantId(),
                                projectId,
                                tenant.adminUserId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCESS_DENIED"));
    }

    @Test
    void crossTenantProjectMembershipAccessIsDenied()
            throws Exception {

        TenantFixture tenantA =
                onboardUniqueTenant("member-tenant-a");

        TenantFixture tenantB =
                onboardUniqueTenant("member-tenant-b");

        String tenantAToken = login(
                tenantA.tenantId(),
                tenantA.adminEmail(),
                ADMIN_PASSWORD
        );

        String tenantBToken = login(
                tenantB.tenantId(),
                tenantB.adminEmail(),
                ADMIN_PASSWORD
        );

        UUID tenantBProjectId = createProject(
                tenantB.tenantId(),
                tenantBToken,
                "Tenant B Membership Project"
        );

        String expectedPath =
                "/api/tenants/"
                        + tenantB.tenantId()
                        + "/projects/"
                        + tenantBProjectId
                        + "/members";

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenantB.tenantId(),
                                tenantBProjectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tenantAToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path")
                        .value(expectedPath));
    }

    @Test
    void archivedProjectMembershipCannotBeModified()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("member-archived");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UserFixture member = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Archived Project Member",
                "archived." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Archived Membership Project"
        );

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}/projects/{projectId}",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("ARCHIVED"));

        String expectedPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/members";

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        projectMemberRequest(
                                                member.userId(),
                                                "MEMBER"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Archived project memberships "
                                        + "cannot be modified"
                        ))
                .andExpect(jsonPath("$.path")
                        .value(expectedPath));
    }

    private TenantFixture onboardUniqueTenant(String prefix)
            throws Exception {

        String suffix = uniqueSuffix();
        String slug = prefix + "-" + suffix;
        String adminEmail =
                "admin." + suffix + "@example.test";

        String requestBody = """
                {
                  "tenantName": "%s",
                  "tenantSlug": "%s",
                  "adminFullName": "Membership Test Administrator",
                  "adminEmail": "%s",
                  "adminPassword": "%s"
                }
                """.formatted(
                prefix + " Tenant",
                slug,
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
                        response.at("/data/tenant/id")
                                .asString()
                ),
                UUID.fromString(
                        response.at("/data/adminUser/id")
                                .asString()
                ),
                adminEmail
        );
    }

    private String login(
            UUID tenantId,
            String email,
            String password
    ) throws Exception {

        String requestBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

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

        return response
                .at("/data/accessToken")
                .asString();
    }

    private UUID createProject(
            UUID tenantId,
            String accessToken,
            String name
    ) throws Exception {

        String requestBody = """
                {
                  "name": "%s",
                  "description": "Project membership integration test."
                }
                """.formatted(name);

        MvcResult result = mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/projects",
                                tenantId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("PLANNING"))
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return UUID.fromString(
                response.at("/data/id").asString()
        );
    }

    private UserFixture inviteAndAcceptUser(
            UUID tenantId,
            String adminToken,
            String fullName,
            String email,
            String tenantRole
    ) throws Exception {

        String invitationRequest = """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "role": "%s"
                }
                """.formatted(
                fullName,
                email,
                tenantRole
        );

        MvcResult invitationResult = mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/user-invitations",
                                tenantId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invitationRequest)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode invitationResponse = jsonMapper.readTree(
                invitationResult
                        .getResponse()
                        .getContentAsString()
        );

        String invitationToken = invitationResponse
                .at("/data/devInvitationToken")
                .asString();

        String acceptRequest = """
                {
                  "invitationToken": "%s",
                  "newPassword": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(
                invitationToken,
                MEMBER_PASSWORD,
                MEMBER_PASSWORD
        );

        MvcResult acceptanceResult = mockMvc.perform(
                        post("/api/user-invitations/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email")
                        .value(email))
                .andExpect(jsonPath("$.data.user.role")
                        .value(tenantRole))
                .andReturn();

        JsonNode acceptanceResponse = jsonMapper.readTree(
                acceptanceResult
                        .getResponse()
                        .getContentAsString()
        );

        return new UserFixture(
                UUID.fromString(
                        acceptanceResponse
                                .at("/data/user/id")
                                .asString()
                ),
                email
        );
    }

    private void addProjectMember(
            UUID tenantId,
            UUID projectId,
            String accessToken,
            UUID userId,
            String projectRole
    ) throws Exception {

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenantId,
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        projectMemberRequest(
                                                userId,
                                                projectRole
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.data.projectRole")
                        .value(projectRole));
    }

    private String projectMemberRequest(
            UUID userId,
            String projectRole
    ) {
        return """
                {
                  "userId": "%s",
                  "role": "%s"
                }
                """.formatted(
                userId,
                projectRole
        );
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);
    }

    private record TenantFixture(
            UUID tenantId,
            UUID adminUserId,
            String adminEmail
    ) {
    }

    private record UserFixture(
            UUID userId,
            String email
    ) {
    }
}