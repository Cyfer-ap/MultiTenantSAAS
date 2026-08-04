package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuthorizationPermissionSource;
import com.chacha.multitenantsaas.entity.AuthorizationScopeType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuthorizationPermissionRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet
        .request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet
        .result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectAuthorizationV2IntegrationTest {

    private static final String PASSWORD =
            "ScopedProject@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TenantOnboardingService
            tenantOnboardingService;

    @Autowired
    private AuthorizationRoleService
            authorizationRoleService;

    @Autowired
    private AuthorizationUserRoleAssignmentService
            assignmentService;

    @Autowired
    private AuthorizationPermissionRepository
            permissionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void projectScopedRoleCannotAccessAnotherProject()
            throws Exception {

        TenantOnboardingResponse onboarding =
                onboardTenant();

        UUID tenantId =
                onboarding.tenant().id();

        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow();

        AppUser administrator =
                appUserRepository
                        .findByTenantIdAndId(
                                tenantId,
                                onboarding.adminUser().id()
                        )
                        .orElseThrow();

        /*
         * The legacy role is deliberately MANAGER.
         *
         * Once this user receives a V2 assignment, the old
         * manager role must not bypass the project scope.
         */
        AppUser scopedOperator =
                createUser(
                        tenant,
                        "Scoped Project Operator",
                        UserRole.TENANT_MANAGER
                );

        Project allowedProject =
                createProject(
                        tenant,
                        administrator,
                        "Allowed Project"
                );

        Project deniedProject =
                createProject(
                        tenant,
                        administrator,
                        "Denied Project"
                );

        AuthorizationRoleResponse role =
                authorizationRoleService
                        .createTenantRole(
                                tenantId,
                                new AuthorizationRoleCreateRequest(
                                        "SCOPED_PROJECT_OPERATOR",
                                        "Scoped Project Operator",
                                        "Can operate one project only.",
                                        Set.of(
                                                getPermissionId(
                                                        PlatformPermissionCodes
                                                                .PROJECT_READ
                                                ),
                                                getPermissionId(
                                                        PlatformPermissionCodes
                                                                .PROJECT_UPDATE
                                                ),
                                                getPermissionId(
                                                        PlatformPermissionCodes
                                                                .PROJECT_MEMBER_MANAGE
                                                ),
                                                getPermissionId(
                                                        PlatformPermissionCodes
                                                                .PROJECT_TASK_READ
                                                ),
                                                getPermissionId(
                                                        PlatformPermissionCodes
                                                                .PROJECT_TASK_MANAGE
                                                )
                                        )
                                )
                        );

        assignmentService.createAssignment(
                tenantId,
                administrator.getId(),
                new AuthorizationUserRoleAssignmentCreateRequest(
                        scopedOperator.getId(),
                        role.id(),
                        AuthorizationScopeType.PROJECT,
                        allowedProject.getId(),
                        Instant.now()
                                .minus(
                                        1,
                                        ChronoUnit.DAYS
                                )
                                .truncatedTo(
                                        ChronoUnit.MICROS
                                ),
                        null
                )
        );

        String accessToken =
                login(
                        tenantId,
                        scopedOperator.getEmail()
                );

        /*
         * A project-scoped role cannot list every project.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}/projects",
                                tenantId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());

        /*
         * Exact project read isolation.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}",
                                tenantId,
                                allowedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.id")
                                .value(
                                        allowedProject
                                                .getId()
                                                .toString()
                                )
                );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}",
                                tenantId,
                                deniedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());

        /*
         * Project update isolation.
         */
        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}",
                                tenantId,
                                allowedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Allowed Project Updated",
                                          "description":
                                            "Updated through scoped V2 access."
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.name")
                                .value(
                                        "Allowed Project Updated"
                                )
                );

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}",
                                tenantId,
                                deniedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Unauthorized Update",
                                          "description": null
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());

        /*
         * Project-member endpoint isolation.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenantId,
                                allowedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/members",
                                tenantId,
                                deniedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());

        /*
         * Task creation and read isolation.
         */
        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenantId,
                                allowedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "title": "Scoped task",
                                          "description":
                                            "Created through V2.",
                                          "priority": "MEDIUM",
                                          "dueAt": null,
                                          "assigneeUserId": null
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.title")
                                .value("Scoped task")
                );

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenantId,
                                deniedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "title": "Unauthorized task",
                                          "description": null,
                                          "priority": "LOW",
                                          "dueAt": null,
                                          "assigneeUserId": null
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenantId,
                                allowedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenantId,
                                deniedProject.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(accessToken)
                                )
                )
                .andExpect(status().isForbidden());
    }

    private UUID getPermissionId(String code) {
        return permissionRepository
                .findBySourceAndCode(
                        AuthorizationPermissionSource.PLATFORM,
                        code
                )
                .orElseThrow()
                .getId();
    }

    private Project createProject(
            Tenant tenant,
            AppUser createdBy,
            String name
    ) {
        Project project = new Project(
                tenant,
                createdBy,
                name,
                "Project authorization V2 test"
        );

        return projectRepository
                .saveAndFlush(project);
    }

    private AppUser createUser(
            Tenant tenant,
            String fullName,
            UserRole legacyRole
    ) {
        String suffix = uniqueSuffix();

        AppUser user = new AppUser(
                tenant,
                fullName,
                "scoped.project."
                        + suffix
                        + "@example.test",
                passwordEncoder.encode(PASSWORD),
                legacyRole
        );

        return appUserRepository
                .saveAndFlush(user);
    }

    private TenantOnboardingResponse onboardTenant() {
        String suffix = uniqueSuffix();

        return tenantOnboardingService
                .onboardTenant(
                        new TenantOnboardingRequest(
                                "Project V2 Tenant",
                                "project-v2-" + suffix,
                                "Project V2 Administrator",
                                "project.v2.admin."
                                        + suffix
                                        + "@example.test",
                                PASSWORD
                        )
                );
    }

    private String login(
            UUID tenantId,
            String email
    ) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/tenants/{tenantId}"
                                                + "/auth/login",
                                        tenantId
                                )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "email": "%s",
                                                  "password": "%s"
                                                }
                                                """.formatted(
                                                        email,
                                                        PASSWORD
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode responseBody =
                jsonMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return responseBody
                .path("data")
                .path("accessToken")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}