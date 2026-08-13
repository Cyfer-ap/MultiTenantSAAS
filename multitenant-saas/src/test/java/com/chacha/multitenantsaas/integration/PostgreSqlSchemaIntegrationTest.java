package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuditLogRepository;
import com.chacha.multitenantsaas.repository.PlatformAuditLogRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.UserInvitationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlSchemaIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("multitenant_saas_test")
                    .withUsername("saas")
                    .withPassword("saas");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add(
                "spring.flyway.locations", () -> "classpath:db/postgresql,classpath:db/common");
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserInvitationRepository userInvitationRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private ProjectTaskRepository projectTaskRepository;
    @Autowired private SystemAdminRepository systemAdminRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PlatformAuditLogRepository platformAuditLogRepository;

    @Test
    void postgresBaselineReachesV17AndMatchesJpaMappings() {
        String version =
                jdbcTemplate.queryForObject(
                        """
                SELECT version
                FROM flyway_schema_history
                WHERE success = TRUE
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                        String.class);

        assertThat(version).isEqualTo("17");

        Integer permissionCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM authorization_permissions", Integer.class);

        assertThat(permissionCount).isEqualTo(20);

        assertTableExists("tenants");
        assertTableExists("app_users");
        assertTableExists("organizational_units");
        assertTableExists("authorization_user_role_assignments");
        assertTableExists("subscription_plans");
        assertTableExists("tenant_subscriptions");
    }

    @Test
    void nullableListFiltersExecuteOnPostgreSql() {
        var pageable = PageRequest.of(0, 20);
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThat(tenantRepository.findTenants(null, null, pageable)).isNotNull();
        assertThat(appUserRepository.findTenantUsers(tenantId, null, null, null, pageable))
                .isNotNull();
        assertThat(projectRepository.findTenantProjects(tenantId, null, null, pageable))
                .isNotNull();
        assertThat(
                        userInvitationRepository.findTenantInvitations(
                                tenantId, null, null, null, pageable))
                .isNotNull();
        assertThat(
                        projectMemberRepository.findProjectMembers(
                                tenantId, projectId, null, null, pageable))
                .isNotNull();
        assertThat(
                        projectTaskRepository.findProjectTasks(
                                tenantId, projectId, null, null, null, null, pageable))
                .isNotNull();
        assertThat(systemAdminRepository.findSystemAdmins(null, null, pageable)).isNotNull();
        assertThat(auditLogRepository.findTenantAuditLogs(tenantId, null, null, pageable))
                .isNotNull();
        assertThat(auditLogRepository.findUserAuditLogs(tenantId, userId, null, null, pageable))
                .isNotNull();
        assertThat(platformAuditLogRepository.findPlatformAuditLogs(null, null, null, pageable))
                .isNotNull();
    }

    private void assertTableExists(String tableName) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = ?
                """,
                        Integer.class,
                        tableName);

        assertThat(count).isEqualTo(1);
    }
}
