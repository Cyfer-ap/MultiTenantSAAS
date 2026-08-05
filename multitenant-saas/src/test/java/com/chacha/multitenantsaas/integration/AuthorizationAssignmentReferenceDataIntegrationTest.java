package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserOrganizationAssignment;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import com.chacha.multitenantsaas.service.AuthorizationProvisioningService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationAssignmentReferenceDataIntegrationTest {

    private static final String PASSWORD =
            "AuthorizationSelectorAdmin@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OrganizationalUnitRepository
            organizationalUnitRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserOrganizationAssignmentRepository
            userOrganizationAssignmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthorizationProvisioningService
            authorizationProvisioningService;

    @Test
    void authorizationManagerReceivesOnlyValidSelectorOptions()
            throws Exception {
        Tenant tenant = createTenant();
        AppUser administrator = createUser(
                tenant,
                "Selector Administrator",
                UserRole.TENANT_ADMIN
        );
        AppUser member = createUser(
                tenant,
                "Grace Selector",
                UserRole.TENANT_USER
        );
        AppUser inactiveUser = createUser(
                tenant,
                "Inactive Selector",
                UserRole.TENANT_USER
        );
        inactiveUser.setStatus(UserStatus.INACTIVE);
        appUserRepository.saveAndFlush(inactiveUser);

        OrganizationalUnit engineering =
                organizationalUnitRepository.saveAndFlush(
                        new OrganizationalUnit(
                                tenant,
                                null,
                                "Engineering",
                                "ENG",
                                OrganizationalUnitType.DEPARTMENT
                        )
                );

        OrganizationalUnit inactiveUnit =
                new OrganizationalUnit(
                        tenant,
                        null,
                        "Former Division",
                        "FORMER",
                        OrganizationalUnitType.DIVISION
                );
        inactiveUnit.setStatus(
                OrganizationalUnitStatus.INACTIVE
        );
        inactiveUnit = organizationalUnitRepository
                .saveAndFlush(inactiveUnit);

        Project activeProject =
                projectRepository.saveAndFlush(
                        new Project(
                                tenant,
                                administrator,
                                "Apollo",
                                "Active selector project."
                        )
                );

        Project archivedProject = new Project(
                tenant,
                administrator,
                "Archived Initiative",
                "Must not be selectable."
        );
        archivedProject.setStatus(ProjectStatus.ARCHIVED);
        archivedProject = projectRepository
                .saveAndFlush(archivedProject);

        UserOrganizationAssignment managerAnchor =
                userOrganizationAssignmentRepository
                        .saveAndFlush(
                                new UserOrganizationAssignment(
                                        tenant,
                                        member,
                                        engineering,
                                        null,
                                        "Engineering Lead",
                                        true,
                                        Instant.now().minus(
                                                1,
                                                ChronoUnit.DAYS
                                        ),
                                        null,
                                        administrator
                                )
                        );

        String adminToken = login(
                tenant.getId(),
                administrator.getEmail()
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization"
                                        + "/assignment-reference-data",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.users[*].id",
                                hasItem(member.getId().toString())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.users[*].id",
                                not(hasItem(
                                        inactiveUser
                                                .getId()
                                                .toString()
                                ))
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.organizationalUnits[*].id",
                                hasItem(engineering.getId().toString())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.organizationalUnits[*].id",
                                not(hasItem(
                                        inactiveUnit
                                                .getId()
                                                .toString()
                                ))
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.projects[*].id",
                                hasItem(activeProject.getId().toString())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.projects[*].id",
                                not(hasItem(
                                        archivedProject
                                                .getId()
                                                .toString()
                                ))
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.directReportsAnchors[0].id"
                        )
                                .value(
                                        managerAnchor
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.data.directReportsAnchors[0]"
                                        + ".ownerUserId"
                        )
                                .value(member.getId().toString())
                )
                .andExpect(
                        jsonPath(
                                "$.data.directReportsAnchors[0].label"
                        )
                                .value(
                                        "Engineering — Engineering Lead"
                                )
                );

        String memberToken = login(
                tenant.getId(),
                member.getEmail()
        );

        mockMvc.perform(
                        get(
                                "/api/tenants/{tenantId}"
                                        + "/authorization"
                                        + "/assignment-reference-data",
                                tenant.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(memberToken)
                                )
                )
                .andExpect(status().isForbidden());
    }

    private AppUser createUser(
            Tenant tenant,
            String fullName,
            UserRole role
    ) {
        AppUser user = new AppUser(
                tenant,
                fullName,
                role.name().toLowerCase().replace('_', '.')
                        + "." + uniqueSuffix()
                        + "@example.test",
                passwordEncoder.encode(PASSWORD),
                role
        );

        AppUser savedUser = appUserRepository
                .saveAndFlush(user);

        authorizationProvisioningService
                .synchronizeUserFromLegacyState(
                        tenant.getId(),
                        savedUser.getId()
                );

        return savedUser;
    }

    private Tenant createTenant() {
        String suffix = uniqueSuffix();
        return tenantRepository.saveAndFlush(
                new Tenant(
                        "Authorization Selector Tenant",
                        "authorization-selector-" + suffix
                )
        );
    }

    private String login(
            UUID tenantId,
            String email
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post(
                                "/api/tenants/{tenantId}/auth/login",
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

        JsonNode responseBody = jsonMapper.readTree(
                result.getResponse().getContentAsString()
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
