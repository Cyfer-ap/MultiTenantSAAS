package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.entity.OrganizationAssignmentStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.AuditLogRepository;
import com.chacha.multitenantsaas.repository.NotificationDeliveryRepository;
import com.chacha.multitenantsaas.repository.NotificationRepository;
import com.chacha.multitenantsaas.repository.PlatformAuditLogRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.UserInvitationRepository;
import com.chacha.multitenantsaas.repository.UserOrganizationAssignmentRepository;
import com.chacha.multitenantsaas.service.NotificationDeliveryService;
import java.time.Instant;
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
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationDeliveryRepository notificationDeliveryRepository;
    @Autowired private NotificationDeliveryService notificationDeliveryService;
    @Autowired private UserOrganizationAssignmentRepository userOrganizationAssignmentRepository;

    @Test
    void postgresSchemaReachesV32AndMatchesJpaMappings() {
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

        assertThat(version).isEqualTo("32");

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
        assertTableExists("email_verification_challenges");
        assertTableExists("trusted_email_browsers");
        assertTableExists("task_comments");
        assertTableExists("task_comment_mentions");
        assertTableExists("task_activities");
        assertTableExists("task_attachments");
        assertTableExists("notifications");
        assertTableExists("notification_deliveries");
        assertTableExists("notification_preferences");
        assertTableExists("billing_customers");
        assertTableExists("billing_events");
        assertTableExists("billing_usage_events");
        assertTableExists("tenant_api_keys");
        assertColumnExists("tenant_api_keys", "tenant_id");
        assertColumnExists("tenant_api_keys", "key_prefix");
        assertColumnExists("tenant_api_keys", "key_hash");
        assertColumnExists("tenant_api_keys", "created_by_user_id");
        assertColumnExists("tenant_api_keys", "last_used_at");
        assertColumnExists("tenant_api_keys", "revoked_at");
        assertColumnExists("billing_usage_events", "tenant_id");
        assertColumnExists("billing_usage_events", "metric_code");
        assertColumnExists("billing_usage_events", "idempotency_key");
        assertColumnExists("billing_usage_events", "occurred_at");
        assertColumnExists("tenant_subscriptions", "billing_provider");
        assertColumnExists("tenant_subscriptions", "provider_subscription_id");
        assertColumnExists("tenant_subscriptions", "provider_event_created_at");
        assertColumnExists("task_attachments", "storage_deleted_at");
        assertColumnExists("task_comments", "parent_comment_id");
        assertColumnExists("task_comments", "reply_count");
        assertColumnExists("task_comments", "pinned_at");
        assertColumnExists("task_comments", "pinned_by_user_id");
        assertColumnExists("notifications", "recipient_user_id");
        assertColumnExists("notifications", "target_url");
        assertColumnExists("notifications", "read_at");
        assertColumnExists("notification_deliveries", "lease_token");
        assertColumnExists("notification_deliveries", "next_attempt_at");
        assertColumnExists("notification_deliveries", "attempt_count");
        assertColumnExists("notification_preferences", "recipient_user_id");
        assertColumnExists("notification_preferences", "type");
        assertColumnExists("notification_preferences", "email_enabled");
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
        assertThat(
                        notificationRepository
                                .findByTenant_IdAndRecipientUser_IdOrderByCreatedAtDesc(
                                        tenantId, userId, pageable))
                .isNotNull();
        assertThat(notificationDeliveryRepository.count()).isZero();
        assertThat(notificationDeliveryService.claimBatch(Instant.now())).isEmpty();
    }

    @Test
    void primaryAssignmentOverlapQueriesExecuteOnPostgreSql() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        var validFrom = java.time.Instant.now();
        var validUntil = validFrom.plusSeconds(3600);

        long openEndedCount =
                userOrganizationAssignmentRepository.countOverlappingOpenEndedPrimaryAssignments(
                        tenantId, userId, OrganizationAssignmentStatus.ACTIVE, validFrom);

        long boundedCount =
                userOrganizationAssignmentRepository.countOverlappingBoundedPrimaryAssignments(
                        tenantId,
                        userId,
                        OrganizationAssignmentStatus.ACTIVE,
                        validFrom,
                        validUntil);

        assertThat(openEndedCount).isZero();
        assertThat(boundedCount).isZero();
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

    private void assertColumnExists(String tableName, String columnName) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """,
                        Integer.class,
                        tableName,
                        columnName);

        assertThat(count).isEqualTo(1);
    }
}
