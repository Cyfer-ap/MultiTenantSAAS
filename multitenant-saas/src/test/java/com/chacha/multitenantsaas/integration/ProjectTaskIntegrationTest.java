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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectTaskIntegrationTest {

    private static final String ADMIN_PASSWORD =
            "TenantAdmin@123";

    private static final String MEMBER_PASSWORD =
            "ProjectMember@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void tenantAdminCanManageCompleteTaskLifecycle()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("task-lifecycle");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Task Lifecycle Project"
        );

        UUID taskId = createTask(
                tenant.tenantId(),
                projectId,
                adminToken,
                "Implement authentication",
                "Create access-token and refresh-token support.",
                "HIGH",
                tenant.adminUserId()
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id")
                        .value(taskId.toString()))
                .andExpect(jsonPath("$.data.projectId")
                        .value(projectId.toString()))
                .andExpect(jsonPath("$.data.tenantId")
                        .value(tenant.tenantId().toString()))
                .andExpect(jsonPath("$.data.title")
                        .value("Implement authentication"))
                .andExpect(jsonPath("$.data.status")
                        .value("TODO"))
                .andExpect(jsonPath("$.data.priority")
                        .value("HIGH"))
                .andExpect(jsonPath("$.data.assigneeUserId")
                        .value(tenant.adminUserId().toString()))
                .andExpect(jsonPath("$.data.createdByUserEmail")
                        .value(tenant.adminEmail()));

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Implement secure authentication",
                                          "description": "Updated authentication implementation.",
                                          "priority": "URGENT",
                                          "dueAt": "2026-08-20T12:00:00Z"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title")
                        .value("Implement secure authentication"))
                .andExpect(jsonPath("$.data.priority")
                        .value("URGENT"))
                .andExpect(jsonPath("$.data.dueAt")
                        .value("2026-08-20T12:00:00Z"));

        updateTaskStatus(
                tenant.tenantId(),
                projectId,
                taskId,
                adminToken,
                "IN_PROGRESS"
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("IN_PROGRESS"));

        updateTaskStatus(
                tenant.tenantId(),
                projectId,
                taskId,
                adminToken,
                "COMPLETED"
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt")
                        .isNotEmpty());

        /*
         * Reopening a completed task clears completedAt.
         */
        updateTaskStatus(
                tenant.tenantId(),
                projectId,
                taskId,
                adminToken,
                "BLOCKED"
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("BLOCKED"))
                .andExpect(jsonPath("$.data.completedAt")
                        .value(nullValue()));

        /*
         * A null assignee unassigns the task.
         */
        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}/assignee",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "assigneeUserId": null
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeUserId")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.assigneeName")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.assigneeEmail")
                        .value(nullValue()));

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("CANCELLED"))
                .andExpect(jsonPath("$.data.completedAt")
                        .value(nullValue()));
    }

    @Test
    void taskListSupportsSearchStatusPriorityAndAssigneeFilters()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("task-filter");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Task Filter Project"
        );

        UUID authenticationTaskId = createTask(
                tenant.tenantId(),
                projectId,
                adminToken,
                "Implement authentication",
                "Create tenant authentication.",
                "HIGH",
                tenant.adminUserId()
        );

        createTask(
                tenant.tenantId(),
                projectId,
                adminToken,
                "Write project documentation",
                "Document project APIs.",
                "LOW",
                null
        );

        updateTaskStatus(
                tenant.tenantId(),
                projectId,
                authenticationTaskId,
                adminToken,
                "IN_PROGRESS"
        ).andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .param("search", "authentication")
                                .param("status", "IN_PROGRESS")
                                .param("priority", "HIGH")
                                .param(
                                        "assigneeUserId",
                                        tenant.adminUserId().toString()
                                )
                                .param("page", "0")
                                .param("size", "10")
                                .param("sortBy", "title")
                                .param("sortDir", "asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.data.content[0].id")
                        .value(authenticationTaskId.toString()))
                .andExpect(jsonPath("$.data.content[0].title")
                        .value("Implement authentication"))
                .andExpect(jsonPath("$.data.content[0].status")
                        .value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.content[0].priority")
                        .value("HIGH"));
    }

    @Test
    void assignedMemberCanUpdateStatusButCannotManageTask()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("task-assigned-member");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UserFixture member = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Assigned Project Member",
                "assigned." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Assigned Member Project"
        );

        addProjectMember(
                tenant.tenantId(),
                projectId,
                adminToken,
                member.userId(),
                "MEMBER"
        );

        UUID taskId = createTask(
                tenant.tenantId(),
                projectId,
                adminToken,
                "Assigned member task",
                "Task assigned to a project member.",
                "MEDIUM",
                member.userId()
        );

        String memberToken = login(
                tenant.tenantId(),
                member.email(),
                MEMBER_PASSWORD
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements")
                        .value(1));

        updateTaskStatus(
                tenant.tenantId(),
                projectId,
                taskId,
                memberToken,
                "IN_PROGRESS"
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("IN_PROGRESS"));

        String taskPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/tasks/"
                        + taskId;

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Forbidden update",
                                          "description": "Not permitted.",
                                          "priority": "LOW",
                                          "dueAt": null
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path")
                        .value(taskPath));

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}/assignee",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "assigneeUserId": null
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCESS_DENIED"));

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCESS_DENIED"));
    }

    @Test
    void unassignedProjectMemberCanReadButCannotUpdateTaskStatus()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("task-unassigned-member");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UserFixture member = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Unassigned Project Member",
                "unassigned." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Unassigned Member Project"
        );

        addProjectMember(
                tenant.tenantId(),
                projectId,
                adminToken,
                member.userId(),
                "MEMBER"
        );

        UUID taskId = createTask(
                tenant.tenantId(),
                projectId,
                adminToken,
                "Administrator task",
                "Assigned to the project administrator.",
                "MEDIUM",
                tenant.adminUserId()
        );

        String memberToken = login(
                tenant.tenantId(),
                member.email(),
                MEMBER_PASSWORD
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + memberToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(taskId.toString()));

        String statusPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/tasks/"
                        + taskId
                        + "/status";

        updateTaskStatus(
                tenant.tenantId(),
                projectId,
                taskId,
                memberToken,
                "IN_PROGRESS"
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path")
                        .value(statusPath));
    }

    @Test
    void projectLeadCanCreateUpdateAssignAndCancelTasks()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("task-project-lead");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UserFixture projectLead = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Project Lead User",
                "lead." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Project Lead Task Project"
        );

        addProjectMember(
                tenant.tenantId(),
                projectId,
                adminToken,
                projectLead.userId(),
                "PROJECT_LEAD"
        );

        String projectLeadToken = login(
                tenant.tenantId(),
                projectLead.email(),
                MEMBER_PASSWORD
        );

        UUID taskId = createTask(
                tenant.tenantId(),
                projectId,
                projectLeadToken,
                "Project lead task",
                "Created by a project lead.",
                "HIGH",
                projectLead.userId()
        );

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + projectLeadToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Updated lead task",
                                          "description": "Updated by the project lead.",
                                          "priority": "URGENT",
                                          "dueAt": null
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title")
                        .value("Updated lead task"))
                .andExpect(jsonPath("$.data.priority")
                        .value("URGENT"));

        mockMvc.perform(
                        patch(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}/assignee",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + projectLeadToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "assigneeUserId": null
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeUserId")
                        .value(nullValue()));

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                taskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + projectLeadToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("CANCELLED"));
    }

    @Test
    void tenantUserWhoIsNotProjectMemberCannotReadTasks()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("task-non-member");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UserFixture tenantUser = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Non Project User",
                "nonmember." + uniqueSuffix() + "@example.test",
                "TENANT_USER"
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Private Project Tasks"
        );

        createTask(
                tenant.tenantId(),
                projectId,
                adminToken,
                "Private task",
                "Only visible to project members.",
                "MEDIUM",
                tenant.adminUserId()
        );

        String userToken = login(
                tenant.tenantId(),
                tenantUser.email(),
                MEMBER_PASSWORD
        );

        String expectedPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/tasks";

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + userToken
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
    void taskAssigneeMustBeActiveProjectMember()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("task-invalid-assignee");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UserFixture tenantUser = inviteAndAcceptUser(
                tenant.tenantId(),
                adminToken,
                "Non Member Assignee",
                "invalid-assignee."
                        + uniqueSuffix()
                        + "@example.test",
                "TENANT_USER"
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Assignee Validation Project"
        );

        String expectedPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/tasks";

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenant.tenantId(),
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(taskRequest(
                                        "Invalid assignment task",
                                        "User is not a project member.",
                                        "HIGH",
                                        tenantUser.userId()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Task assignee must be a member "
                                        + "of the project"
                        ))
                .andExpect(jsonPath("$.path")
                        .value(expectedPath));
    }

    @Test
    void crossTenantTaskAccessIsDenied()
            throws Exception {

        TenantFixture tenantA =
                onboardUniqueTenant("task-tenant-a");

        TenantFixture tenantB =
                onboardUniqueTenant("task-tenant-b");

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
                "Tenant B Task Project"
        );

        createTask(
                tenantB.tenantId(),
                tenantBProjectId,
                tenantBToken,
                "Tenant B task",
                "Private to tenant B.",
                "MEDIUM",
                tenantB.adminUserId()
        );

        String expectedPath =
                "/api/tenants/"
                        + tenantB.tenantId()
                        + "/projects/"
                        + tenantBProjectId
                        + "/tasks";

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenantB.tenantId(),
                                tenantBProjectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tenantAToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode")
                        .value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path")
                        .value(expectedPath));
    }

    @Test
    void cancelledTasksAndArchivedProjectTasksCannotBeModified()
            throws Exception {

        TenantFixture tenant =
                onboardUniqueTenant("task-immutable");

        String adminToken = login(
                tenant.tenantId(),
                tenant.adminEmail(),
                ADMIN_PASSWORD
        );

        UUID projectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Immutable Task Project"
        );

        UUID cancelledTaskId = createTask(
                tenant.tenantId(),
                projectId,
                adminToken,
                "Task to cancel",
                "Cancelled tasks are immutable.",
                "LOW",
                tenant.adminUserId()
        );

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                cancelledTaskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk());

        String cancelledTaskPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + projectId
                        + "/tasks/"
                        + cancelledTaskId;

        mockMvc.perform(
                        put(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                projectId,
                                cancelledTaskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Modified cancelled task",
                                          "description": "Not allowed.",
                                          "priority": "HIGH",
                                          "dueAt": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Cancelled task cannot be modified"
                        ))
                .andExpect(jsonPath("$.path")
                        .value(cancelledTaskPath));

        UUID archivedProjectId = createProject(
                tenant.tenantId(),
                adminToken,
                "Archived Task Project"
        );

        UUID archivedProjectTaskId = createTask(
                tenant.tenantId(),
                archivedProjectId,
                adminToken,
                "Existing archived project task",
                "Created before project archival.",
                "MEDIUM",
                tenant.adminUserId()
        );

        mockMvc.perform(
                        delete(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}",
                                tenant.tenantId(),
                                archivedProjectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("ARCHIVED"));

        String archivedTaskCollectionPath =
                "/api/tenants/"
                        + tenant.tenantId()
                        + "/projects/"
                        + archivedProjectId
                        + "/tasks";

        mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenant.tenantId(),
                                archivedProjectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(taskRequest(
                                        "New archived task",
                                        "Must not be created.",
                                        "MEDIUM",
                                        tenant.adminUserId()
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Archived project tasks cannot be modified"
                        ))
                .andExpect(jsonPath("$.path")
                        .value(archivedTaskCollectionPath));

        /*
         * Existing tasks remain readable for history.
         */
        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}"
                                        + "/tasks/{taskId}",
                                tenant.tenantId(),
                                archivedProjectId,
                                archivedProjectTaskId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(archivedProjectTaskId.toString()));
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
                  "adminFullName": "Task Test Administrator",
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
                                .content("""
                                        {
                                          "name": "%s",
                                          "description": "Task integration project."
                                        }
                                        """.formatted(name))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return UUID.fromString(
                response.at("/data/id").asString()
        );
    }

    private UUID createTask(
            UUID tenantId,
            UUID projectId,
            String accessToken,
            String title,
            String description,
            String priority,
            UUID assigneeUserId
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}"
                                        + "/projects/{projectId}/tasks",
                                tenantId,
                                projectId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(taskRequest(
                                        title,
                                        description,
                                        priority,
                                        assigneeUserId
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title")
                        .value(title))
                .andExpect(jsonPath("$.data.priority")
                        .value(priority))
                .andExpect(jsonPath("$.data.status")
                        .value("TODO"))
                .andReturn();

        JsonNode response = jsonMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return UUID.fromString(
                response.at("/data/id").asString()
        );
    }

    private String taskRequest(
            String title,
            String description,
            String priority,
            UUID assigneeUserId
    ) {
        String assigneeJson = assigneeUserId == null
                ? "null"
                : "\"" + assigneeUserId + "\"";

        return """
                {
                  "title": "%s",
                  "description": "%s",
                  "priority": "%s",
                  "dueAt": "2026-08-15T12:00:00Z",
                  "assigneeUserId": %s
                }
                """.formatted(
                title,
                description,
                priority,
                assigneeJson
        );
    }

    private org.springframework.test.web.servlet.ResultActions
    updateTaskStatus(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            String accessToken,
            String status
    ) throws Exception {

        return mockMvc.perform(
                patch(
                        "/api/tenants/{tenantId}"
                                + "/projects/{projectId}"
                                + "/tasks/{taskId}/status",
                        tenantId,
                        projectId,
                        taskId
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "%s"
                                }
                                """.formatted(status))
        );
    }

    private UserFixture inviteAndAcceptUser(
            UUID tenantId,
            String adminToken,
            String fullName,
            String email,
            String tenantRole
    ) throws Exception {

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
                                .content("""
                                        {
                                          "fullName": "%s",
                                          "email": "%s",
                                          "role": "%s"
                                        }
                                        """.formatted(
                                        fullName,
                                        email,
                                        tenantRole
                                ))
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

        MvcResult acceptanceResult = mockMvc.perform(
                        post("/api/user-invitations/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "invitationToken": "%s",
                                          "newPassword": "%s",
                                          "confirmPassword": "%s"
                                        }
                                        """.formatted(
                                        invitationToken,
                                        MEMBER_PASSWORD,
                                        MEMBER_PASSWORD
                                ))
                )
                .andExpect(status().isOk())
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
            String role
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
                                .content("""
                                        {
                                          "userId": "%s",
                                          "role": "%s"
                                        }
                                        """.formatted(
                                        userId,
                                        role
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId")
                        .value(userId.toString()))
                .andExpect(jsonPath("$.data.projectRole")
                        .value(role));
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