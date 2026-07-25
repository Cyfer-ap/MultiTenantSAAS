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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserInvitationIntegrationTest {

    private static final String ADMIN_PASSWORD =
            "TenantAdmin@123";

    private static final String INVITED_USER_PASSWORD =
            "InvitedUser@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void tenantAdminCanCreateListAcceptAndLoginInvitedUser()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("invitation-accept");

        String adminAccessToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        String invitedEmail =
                "manager." + uniqueSuffix() + "@example.test";

        InvitationFixture invitation = createInvitation(
                tenant.tenantId(),
                adminAccessToken,
                "Invited Tenant Manager",
                invitedEmail,
                "TENANT_MANAGER"
        );

        /*
         * Verify invitation listing, filtering,
         * pagination and inviter information.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}/user-invitations",
                                tenant.tenantId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "createdAt")
                                .param("sortDir", "desc")
                                .param("status", "PENDING")
                                .param("role", "TENANT_MANAGER")
                                .param("search", invitedEmail)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.data.content[0].invitationId")
                        .value(invitation.invitationId().toString()))
                .andExpect(jsonPath("$.data.content[0].email")
                        .value(invitedEmail))
                .andExpect(jsonPath("$.data.content[0].role")
                        .value("TENANT_MANAGER"))
                .andExpect(jsonPath("$.data.content[0].status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].active")
                        .value(true))
                .andExpect(jsonPath("$.data.content[0].expired")
                        .value(false))
                .andExpect(jsonPath(
                        "$.data.content[0].invitedByUserEmail"
                ).value(tenant.adminEmail()))
                .andExpect(jsonPath(
                        "$.data.content[0].devInvitationToken"
                ).doesNotExist());

        /*
         * Verify invitation detail endpoint.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/user-invitations/{invitationId}",
                                tenant.tenantId(),
                                invitation.invitationId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invitationId")
                        .value(invitation.invitationId().toString()))
                .andExpect(jsonPath("$.data.email")
                        .value(invitedEmail))
                .andExpect(jsonPath("$.data.status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.data.active")
                        .value(true));

        acceptInvitation(
                invitation.invitationToken(),
                INVITED_USER_PASSWORD,
                invitedEmail,
                "TENANT_MANAGER"
        );

        /*
         * Verify that the invitation is now accepted.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/user-invitations/{invitationId}",
                                tenant.tenantId(),
                                invitation.invitationId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("ACCEPTED"))
                .andExpect(jsonPath("$.data.active")
                        .value(false))
                .andExpect(jsonPath("$.data.acceptedAt")
                        .isNotEmpty())
                .andExpect(jsonPath("$.data.revokedAt")
                        .doesNotExist());

        /*
         * The accepted user must now be able to log in.
         */
        String invitedUserAccessToken = login(
                tenant.tenantId(),
                invitedEmail,
                INVITED_USER_PASSWORD
        );

        mockMvc.perform(
                        get("/api/auth/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer "
                                                + invitedUserAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId")
                        .value(tenant.tenantId().toString()))
                .andExpect(jsonPath("$.data.email")
                        .value(invitedEmail))
                .andExpect(jsonPath("$.data.role")
                        .value("TENANT_MANAGER"))
                .andExpect(jsonPath("$.data.status")
                        .value("ACTIVE"));

        /*
         * Invitation tokens are one-time use.
         */
        assertInvitationRejected(
                invitation.invitationToken()
        );
    }

    @Test
    void revokedInvitationCannotBeAccepted()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("invitation-revoke");

        String adminAccessToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        String invitedEmail =
                "revoked." + uniqueSuffix() + "@example.test";

        InvitationFixture invitation = createInvitation(
                tenant.tenantId(),
                adminAccessToken,
                "Revoked Invitation User",
                invitedEmail,
                "TENANT_USER"
        );

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/user-invitations/{invitationId}"
                                        + "/revoke",
                                tenant.tenantId(),
                                invitation.invitationId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status")
                        .value("REVOKED"))
                .andExpect(jsonPath("$.data.active")
                        .value(false))
                .andExpect(jsonPath("$.data.revokedAt")
                        .isNotEmpty());

        assertInvitationRejected(
                invitation.invitationToken()
        );
    }

    @Test
    void creatingReplacementInvitationRevokesPreviousPendingInvitation()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("invitation-replacement");

        String adminAccessToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        String invitedEmail =
                "replacement." + uniqueSuffix() + "@example.test";

        InvitationFixture firstInvitation = createInvitation(
                tenant.tenantId(),
                adminAccessToken,
                "Replacement User",
                invitedEmail,
                "TENANT_USER"
        );

        InvitationFixture secondInvitation = createInvitation(
                tenant.tenantId(),
                adminAccessToken,
                "Replacement User",
                invitedEmail,
                "TENANT_USER"
        );

        assertNotEquals(
                firstInvitation.invitationToken(),
                secondInvitation.invitationToken()
        );

        /*
         * Creating the replacement invitation must revoke
         * the previously pending invitation.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/user-invitations/{invitationId}",
                                tenant.tenantId(),
                                firstInvitation.invitationId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("REVOKED"))
                .andExpect(jsonPath("$.data.active")
                        .value(false))
                .andExpect(jsonPath("$.data.revokedAt")
                        .isNotEmpty());

        assertInvitationRejected(
                firstInvitation.invitationToken()
        );

        /*
         * The replacement token must remain valid.
         */
        acceptInvitation(
                secondInvitation.invitationToken(),
                INVITED_USER_PASSWORD,
                invitedEmail,
                "TENANT_USER"
        );
    }

    @Test
    void tenantAdminCannotManageAnotherTenantsInvitations()
            throws Exception {

        TenantFixture tenantA =
                onboardUniqueTenant("invitation-tenant-a");

        TenantFixture tenantB =
                onboardUniqueTenant("invitation-tenant-b");

        String tenantAAccessToken = login(
                tenantA.tenantId(),
                tenantA.adminEmail(),
                ADMIN_PASSWORD
        );

        String requestBody = invitationRequest(
                "Cross Tenant User",
                "cross." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        String expectedPath =
                "/api/tenants/"
                        + tenantB.tenantId()
                        + "/user-invitations";

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/user-invitations",
                                tenantB.tenantId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tenantAAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
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
    void existingTenantUserCannotBeInvitedAgain()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("invitation-existing-user");

        String adminAccessToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        String requestBody = invitationRequest(
                "Existing Administrator",
                tenant.adminEmail(),
                "TENANT_USER"
        );

        String expectedPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/user-invitations";

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/user-invitations",
                                tenant.tenantId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
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
                  "adminFullName": "Invitation Test Administrator",
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
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        UUID tenantId = UUID.fromString(
                response.at("/data/tenant/id").asString()
        );

        return new TenantFixture(
                tenantId,
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email")
                        .value(email))
                .andExpect(jsonPath("$.data.accessToken")
                        .isNotEmpty())
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return response
                .at("/data/accessToken")
                .asString();
    }

    private InvitationFixture createInvitation(
            UUID tenantId,
            String adminAccessToken,
            String fullName,
            String email,
            String role
    ) throws Exception {

        String requestBody = invitationRequest(
                fullName,
                email,
                role
        );

        MvcResult result = mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/user-invitations",
                                tenantId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantId")
                        .value(tenantId.toString()))
                .andExpect(jsonPath("$.data.email")
                        .value(email))
                .andExpect(jsonPath("$.data.role")
                        .value(role))
                .andExpect(jsonPath("$.data.status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.data.devInvitationToken")
                        .isNotEmpty())
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        UUID invitationId = UUID.fromString(
                response.at("/data/invitationId").asString()
        );

        String invitationToken = response
                .at("/data/devInvitationToken")
                .asString();

        return new InvitationFixture(
                invitationId,
                invitationToken
        );
    }

    private void acceptInvitation(
            String invitationToken,
            String password,
            String expectedEmail,
            String expectedRole
    ) throws Exception {

        String requestBody = """
                {
                  "invitationToken": "%s",
                  "newPassword": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(
                invitationToken,
                password,
                password
        );

        mockMvc.perform(
                        post("/api/user-invitations/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email")
                        .value(expectedEmail))
                .andExpect(jsonPath("$.data.user.role")
                        .value(expectedRole))
                .andExpect(jsonPath("$.data.user.status")
                        .value("ACTIVE"));
    }

    private void assertInvitationRejected(
            String invitationToken
    ) throws Exception {

        String requestBody = """
                {
                  "invitationToken": "%s",
                  "newPassword": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(
                invitationToken,
                INVITED_USER_PASSWORD,
                INVITED_USER_PASSWORD
        );

        mockMvc.perform(
                        post("/api/user-invitations/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode")
                        .value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path")
                        .value("/api/user-invitations/accept"));
    }

    private String invitationRequest(
            String fullName,
            String email,
            String role
    ) {
        return """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "role": "%s"
                }
                """.formatted(
                fullName,
                email,
                role
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
            String adminEmail
    ) {
    }

    private record InvitationFixture(
            UUID invitationId,
            String invitationToken
    ) {
    }
}