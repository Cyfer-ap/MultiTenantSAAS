package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chacha.multitenantsaas.dto.AuthorizationRoleCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationRoleResponse;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentCreateRequest;
import com.chacha.multitenantsaas.dto.AuthorizationUserRoleAssignmentResponse;
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
import com.chacha.multitenantsaas.security.AuthorizationAccessDecisionReason;
import com.chacha.multitenantsaas.security.AuthorizationEvaluationContext;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import com.chacha.multitenantsaas.service.AuthorizationPermissionEvaluator;
import com.chacha.multitenantsaas.service.AuthorizationRoleService;
import com.chacha.multitenantsaas.service.AuthorizationUserRoleAssignmentService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationDelegationIntegrationTest {

    private static final String PASSWORD = "Delegation@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private TenantOnboardingService tenantOnboardingService;
    @Autowired private AuthorizationRoleService authorizationRoleService;
    @Autowired private AuthorizationUserRoleAssignmentService assignmentService;
    @Autowired private AuthorizationPermissionEvaluator authorizationPermissionEvaluator;
    @Autowired private AuthorizationPermissionRepository permissionRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void createsBoundedProjectDelegationAndRevocationRemovesAccess() throws Exception {
        TestContext context = createContext("delegation-lifecycle");
        AppUser delegator = createUser(context.tenant(), "Delegation Manager");
        AppUser delegate = createUser(context.tenant(), "Delegation Lead");
        Project project = createProject(context, "Delegated Project");
        Project otherProject = createProject(context, "Other Project");

        AuthorizationRoleResponse parentRole =
                createRole(
                        context.tenant(),
                        "DELEGATION_PARENT",
                        Set.of(
                                PlatformPermissionCodes.AUTHORIZATION_DELEGATE,
                                PlatformPermissionCodes.PROJECT_TASK_READ));
        AuthorizationRoleResponse childRole =
                createRole(
                        context.tenant(),
                        "DELEGATED_TASK_READER",
                        Set.of(PlatformPermissionCodes.PROJECT_TASK_READ));
        AuthorizationUserRoleAssignmentResponse parentAssignment =
                assignRole(
                        context, delegator, parentRole, AuthorizationScopeType.TENANT, null, null);

        String token = login(context.tenant().getId(), delegator.getEmail());
        Instant validUntil = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

        MvcResult createResult =
                createDelegation(
                                token,
                                context.tenant().getId(),
                                delegate.getId(),
                                parentAssignment.id(),
                                childRole.id(),
                                AuthorizationScopeType.PROJECT,
                                project.getId(),
                                validUntil)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andExpect(jsonPath("$.data.roleCode").value("DELEGATED_TASK_READER"))
                        .andExpect(jsonPath("$.data.scopeType").value("PROJECT"))
                        .andExpect(
                                jsonPath("$.data.scopeTargetId").value(project.getId().toString()))
                        .andExpect(
                                jsonPath("$.data.parentAssignmentId")
                                        .value(parentAssignment.id().toString()))
                        .andReturn();

        JsonNode createResponse =
                jsonMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        UUID delegationId = UUID.fromString(createResponse.path("id").asText());

        assertThat(
                        authorizationPermissionEvaluator.hasPermission(
                                context.tenant().getId(),
                                delegate.getId(),
                                PlatformPermissionCodes.PROJECT_TASK_READ,
                                AuthorizationEvaluationContext.project(project.getId())))
                .isTrue();
        assertThat(
                        authorizationPermissionEvaluator.hasPermission(
                                context.tenant().getId(),
                                delegate.getId(),
                                PlatformPermissionCodes.PROJECT_TASK_READ,
                                AuthorizationEvaluationContext.project(otherProject.getId())))
                .isFalse();

        String administratorToken =
                login(context.tenant().getId(), context.administrator().getEmail());
        mockMvc.perform(
                        post(
                                        "/api/tenants/{tenantId}/authorization/explain-access",
                                        context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(administratorToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "userId": "%s",
                                          "permissionCode": "%s",
                                          "contextType": "PROJECT",
                                          "targetId": "%s"
                                        }
                                        """
                                                .formatted(
                                                        delegate.getId(),
                                                        PlatformPermissionCodes.PROJECT_TASK_READ,
                                                        project.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.granted").value(true))
                .andExpect(jsonPath("$.data.matchedGrant.grantSource").value("DELEGATED"))
                .andExpect(
                        jsonPath("$.data.matchedGrant.delegationId")
                                .value(delegationId.toString()))
                .andExpect(
                        jsonPath("$.data.matchedGrant.parentAssignmentId")
                                .value(parentAssignment.id().toString()))
                .andExpect(
                        jsonPath("$.data.matchedGrant.delegatorUserId")
                                .value(delegator.getId().toString()))
                .andExpect(
                        jsonPath("$.data.matchedGrant.delegatorEmail")
                                .value(delegator.getEmail()));

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/authorization/delegations",
                                        context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(delegationId.toString()));

        mockMvc.perform(
                        patch(
                                        "/api/tenants/{tenantId}/authorization/delegations/{delegationId}/revoke",
                                        context.tenant().getId(),
                                        delegationId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));

        assertThat(
                        authorizationPermissionEvaluator.hasPermission(
                                context.tenant().getId(),
                                delegate.getId(),
                                PlatformPermissionCodes.PROJECT_TASK_READ,
                                AuthorizationEvaluationContext.project(project.getId())))
                .isFalse();
    }

    @Test
    void delegateOnlyReferenceDataExposesBoundedChoicesWithoutManagementAccess() throws Exception {
        TestContext context = createContext("delegation-reference");
        AppUser delegator = createUser(context.tenant(), "Reference Delegator");
        AppUser delegate = createUser(context.tenant(), "Reference Delegate");

        AuthorizationRoleResponse parentRole =
                createRole(
                        context.tenant(),
                        "REFERENCE_PARENT",
                        Set.of(
                                PlatformPermissionCodes.AUTHORIZATION_DELEGATE,
                                PlatformPermissionCodes.PROJECT_TASK_READ));
        AuthorizationRoleResponse childRole =
                createRole(
                        context.tenant(),
                        "REFERENCE_CHILD",
                        Set.of(PlatformPermissionCodes.PROJECT_TASK_READ));
        AuthorizationUserRoleAssignmentResponse parentAssignment =
                assignRole(
                        context, delegator, parentRole, AuthorizationScopeType.TENANT, null, null);

        String token = login(context.tenant().getId(), delegator.getEmail());

        MvcResult referenceResult =
                mockMvc.perform(
                                get(
                                                "/api/tenants/{tenantId}/authorization/delegations/reference-data",
                                                context.tenant().getId())
                                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode referenceData =
                jsonMapper.readTree(referenceResult.getResponse().getContentAsString()).path("data");

        assertThat(fieldValues(referenceData.path("users"), "id"))
                .contains(delegate.getId().toString())
                .doesNotContain(delegator.getId().toString());
        assertThat(fieldValues(referenceData.path("roles"), "id"))
                .contains(childRole.id().toString())
                .doesNotContain(parentRole.id().toString());
        assertThat(fieldValues(referenceData.path("parentAssignments"), "id"))
                .containsExactly(parentAssignment.id().toString());

        mockMvc.perform(
                        get(
                                        "/api/tenants/{tenantId}/authorization/assignment-reference-data",
                                        context.tenant().getId())
                                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidParentAuthorityImmediatelyInvalidatesDelegatedGrant() throws Exception {
        TestContext context = createContext("delegation-parent");
        AppUser delegator = createUser(context.tenant(), "Source Manager");
        AppUser delegate = createUser(context.tenant(), "Source Delegate");
        Project project = createProject(context, "Source Project");

        AuthorizationRoleResponse parentRole =
                createRole(
                        context.tenant(),
                        "SOURCE_PARENT",
                        Set.of(
                                PlatformPermissionCodes.AUTHORIZATION_DELEGATE,
                                PlatformPermissionCodes.PROJECT_TASK_READ));
        AuthorizationRoleResponse childRole =
                createRole(
                        context.tenant(),
                        "SOURCE_CHILD",
                        Set.of(PlatformPermissionCodes.PROJECT_TASK_READ));
        AuthorizationUserRoleAssignmentResponse parentAssignment =
                assignRole(
                        context, delegator, parentRole, AuthorizationScopeType.TENANT, null, null);

        String token = login(context.tenant().getId(), delegator.getEmail());
        createDelegation(
                        token,
                        context.tenant().getId(),
                        delegate.getId(),
                        parentAssignment.id(),
                        childRole.id(),
                        AuthorizationScopeType.PROJECT,
                        project.getId(),
                        Instant.now().plus(2, ChronoUnit.DAYS))
                .andExpect(status().isOk());

        assertThat(
                        authorizationPermissionEvaluator.hasPermission(
                                context.tenant().getId(),
                                delegate.getId(),
                                PlatformPermissionCodes.PROJECT_TASK_READ,
                                AuthorizationEvaluationContext.project(project.getId())))
                .isTrue();

        assignmentService.deactivateAssignment(context.tenant().getId(), parentAssignment.id());

        var decision =
                authorizationPermissionEvaluator.evaluatePermission(
                        context.tenant().getId(),
                        delegate.getId(),
                        PlatformPermissionCodes.PROJECT_TASK_READ,
                        AuthorizationEvaluationContext.project(project.getId()));

        assertThat(decision.granted()).isFalse();
        assertThat(decision.reason())
                .isEqualTo(AuthorizationAccessDecisionReason.DELEGATION_SOURCE_UNAVAILABLE);
    }

    @Test
    void rejectsPermissionScopeTimeAndRedelegationEscalation() throws Exception {
        TestContext context = createContext("delegation-boundary");
        AppUser delegator = createUser(context.tenant(), "Boundary Manager");
        AppUser firstDelegate = createUser(context.tenant(), "Boundary Lead");
        AppUser secondDelegate = createUser(context.tenant(), "Boundary Engineer");
        Project project = createProject(context, "Boundary Project");

        AuthorizationRoleResponse capabilityRole =
                createRole(
                        context.tenant(),
                        "DELEGATION_CAPABILITY",
                        Set.of(PlatformPermissionCodes.AUTHORIZATION_DELEGATE));
        assignRole(context, delegator, capabilityRole, AuthorizationScopeType.TENANT, null, null);

        AuthorizationRoleResponse parentRole =
                createRole(
                        context.tenant(),
                        "PROJECT_READER_PARENT",
                        Set.of(PlatformPermissionCodes.PROJECT_TASK_READ));
        Instant parentValidUntil =
                Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        AuthorizationUserRoleAssignmentResponse parentAssignment =
                assignRole(
                        context,
                        delegator,
                        parentRole,
                        AuthorizationScopeType.PROJECT,
                        project.getId(),
                        parentValidUntil);

        AuthorizationRoleResponse readRole =
                createRole(
                        context.tenant(),
                        "BOUNDARY_READER",
                        Set.of(PlatformPermissionCodes.PROJECT_TASK_READ));
        AuthorizationRoleResponse manageRole =
                createRole(
                        context.tenant(),
                        "BOUNDARY_MANAGER",
                        Set.of(PlatformPermissionCodes.PROJECT_TASK_MANAGE));
        AuthorizationRoleResponse delegateRole =
                createRole(
                        context.tenant(),
                        "BOUNDARY_DELEGATOR",
                        Set.of(PlatformPermissionCodes.AUTHORIZATION_DELEGATE));

        String token = login(context.tenant().getId(), delegator.getEmail());

        createDelegation(
                        token,
                        context.tenant().getId(),
                        firstDelegate.getId(),
                        parentAssignment.id(),
                        manageRole.id(),
                        AuthorizationScopeType.PROJECT,
                        project.getId(),
                        parentValidUntil.minus(1, ChronoUnit.HOURS))
                .andExpect(status().isBadRequest());

        createDelegation(
                        token,
                        context.tenant().getId(),
                        firstDelegate.getId(),
                        parentAssignment.id(),
                        delegateRole.id(),
                        AuthorizationScopeType.PROJECT,
                        project.getId(),
                        parentValidUntil.minus(1, ChronoUnit.HOURS))
                .andExpect(status().isBadRequest());

        createDelegation(
                        token,
                        context.tenant().getId(),
                        firstDelegate.getId(),
                        parentAssignment.id(),
                        readRole.id(),
                        AuthorizationScopeType.TENANT,
                        null,
                        parentValidUntil.minus(1, ChronoUnit.HOURS))
                .andExpect(status().isBadRequest());

        createDelegation(
                        token,
                        context.tenant().getId(),
                        firstDelegate.getId(),
                        parentAssignment.id(),
                        readRole.id(),
                        AuthorizationScopeType.PROJECT,
                        project.getId(),
                        parentValidUntil.plus(1, ChronoUnit.HOURS))
                .andExpect(status().isBadRequest());

        MvcResult validDelegation =
                createDelegation(
                                token,
                                context.tenant().getId(),
                                firstDelegate.getId(),
                                parentAssignment.id(),
                                readRole.id(),
                                AuthorizationScopeType.PROJECT,
                                project.getId(),
                                parentValidUntil.minus(1, ChronoUnit.HOURS))
                        .andExpect(status().isOk())
                        .andReturn();
        UUID delegatedAssignmentId =
                UUID.fromString(
                        jsonMapper
                                .readTree(validDelegation.getResponse().getContentAsString())
                                .path("data")
                                .path("delegatedAssignmentId")
                                .asText());

        assignRole(
                context, firstDelegate, capabilityRole, AuthorizationScopeType.TENANT, null, null);
        String firstDelegateToken = login(context.tenant().getId(), firstDelegate.getEmail());

        createDelegation(
                        firstDelegateToken,
                        context.tenant().getId(),
                        secondDelegate.getId(),
                        delegatedAssignmentId,
                        readRole.id(),
                        AuthorizationScopeType.PROJECT,
                        project.getId(),
                        parentValidUntil.minus(2, ChronoUnit.HOURS))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions createDelegation(
            String token,
            UUID tenantId,
            UUID delegateUserId,
            UUID parentAssignmentId,
            UUID roleId,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId,
            Instant validUntil)
            throws Exception {
        String scopeTargetJson = scopeTargetId == null ? "null" : "\"" + scopeTargetId + "\"";
        return mockMvc.perform(
                post("/api/tenants/{tenantId}/authorization/delegations", tenantId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "delegateUserId": "%s",
                                  "parentAssignmentId": "%s",
                                  "roleId": "%s",
                                  "scopeType": "%s",
                                  "scopeTargetId": %s,
                                  "validUntil": "%s"
                                }
                                """
                                        .formatted(
                                                delegateUserId,
                                                parentAssignmentId,
                                                roleId,
                                                scopeType,
                                                scopeTargetJson,
                                                validUntil)));
    }

    private TestContext createContext(String prefix) {
        String suffix = uniqueSuffix();
        TenantOnboardingResponse onboarding =
                tenantOnboardingService.onboardTenant(
                        new TenantOnboardingRequest(
                                prefix + " Tenant",
                                prefix + "-" + suffix,
                                prefix + " Administrator",
                                prefix + ".admin." + suffix + "@example.test",
                                PASSWORD));
        Tenant tenant = tenantRepository.findById(onboarding.tenant().id()).orElseThrow();
        AppUser administrator =
                appUserRepository
                        .findByTenantIdAndId(tenant.getId(), onboarding.adminUser().id())
                        .orElseThrow();
        return new TestContext(tenant, administrator);
    }

    private AppUser createUser(Tenant tenant, String fullName) {
        String suffix = uniqueSuffix();
        AppUser user =
                new AppUser(
                        tenant,
                        fullName,
                        "delegation." + suffix + "@example.test",
                        passwordEncoder.encode(PASSWORD),
                        UserRole.TENANT_USER);
        return appUserRepository.saveAndFlush(user);
    }

    private Project createProject(TestContext context, String name) {
        return projectRepository.saveAndFlush(
                new Project(context.tenant(), context.administrator(), name, null));
    }

    private AuthorizationRoleResponse createRole(
            Tenant tenant, String roleCode, Set<String> permissionCodes) {
        Set<UUID> permissionIds =
                permissionCodes.stream()
                        .map(this::getPermissionId)
                        .collect(java.util.stream.Collectors.toSet());
        return authorizationRoleService.createTenantRole(
                tenant.getId(),
                new AuthorizationRoleCreateRequest(
                        roleCode, roleCode.replace('_', ' '), null, permissionIds));
    }

    private UUID getPermissionId(String code) {
        return permissionRepository
                .findBySourceAndCode(AuthorizationPermissionSource.PLATFORM, code)
                .orElseThrow()
                .getId();
    }

    private AuthorizationUserRoleAssignmentResponse assignRole(
            TestContext context,
            AppUser user,
            AuthorizationRoleResponse role,
            AuthorizationScopeType scopeType,
            UUID scopeTargetId,
            Instant validUntil) {
        return assignmentService.createAssignment(
                context.tenant().getId(),
                context.administrator().getId(),
                new AuthorizationUserRoleAssignmentCreateRequest(
                        user.getId(),
                        role.id(),
                        scopeType,
                        scopeTargetId,
                        Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS),
                        validUntil));
    }

    private Set<String> fieldValues(JsonNode array, String fieldName) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(item -> item.path(fieldName).asText())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String login(UUID tenantId, String email) throws Exception {
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
                                                        .formatted(email, PASSWORD)))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode response = jsonMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record TestContext(Tenant tenant, AppUser administrator) {}
}
