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
import java.util.List;
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
    void postgresSchemaReachesV54AndMatchesJpaMappings() {
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

        assertThat(version).isEqualTo("54");

        Integer permissionCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM authorization_permissions", Integer.class);
        assertThat(permissionCount).isEqualTo(21);

        List.of(
                        "tenants",
                        "app_users",
                        "organizational_units",
                        "authorization_user_role_assignments",
                        "authorization_delegations",
                        "subscription_plans",
                        "tenant_subscriptions",
                        "tenant_subscription_history",
                        "subscription_plan_provider_mappings",
                        "subscription_plan_retirement_operations",
                        "email_verification_challenges",
                        "trusted_email_browsers",
                        "task_comments",
                        "task_comment_mentions",
                        "task_activities",
                        "task_attachments",
                        "notifications",
                        "notification_deliveries",
                        "notification_preferences",
                        "billing_customers",
                        "billing_events",
                        "billing_usage_events",
                        "tenant_api_keys",
                        "subscription_plan_usage_limits",
                        "outbound_webhook_endpoints",
                        "outbound_webhook_endpoint_events",
                        "outbound_webhook_events",
                        "outbound_webhook_deliveries",
                        "outbound_webhook_delivery_attempts",
                        "tenant_identity_providers",
                        "tenant_identity_provider_scopes",
                        "oidc_authorization_transactions",
                        "tenant_federated_identities",
                        "oidc_session_handoffs",
                        "personal_workspace_items",
                        "saved_views",
                        "task_dependencies",
                        "project_task_labels",
                        "project_task_label_assignments",
                        "recurring_task_definitions",
                        "recurring_task_occurrences",
                        "project_task_templates",
                        "project_templates",
                        "project_template_tasks",
                        "workflow_definitions",
                        "workflow_nodes",
                        "workflow_edges",
                        "workflow_executions",
                        "whiteboards",
                        "whiteboard_nodes",
                        "whiteboard_edges",
                        "form_definitions",
                        "form_fields",
                        "form_submissions",
                        "approval_definitions",
                        "approval_stages",
                        "approval_stage_reviewers",
                        "approval_requests",
                        "approval_request_stages",
                        "approval_request_stage_reviewers",
                        "external_access_grants",
                        "external_access_grant_capabilities",
                        "external_guest_sessions")
                .forEach(this::assertTableExists);

        List<String[]> requiredColumns =
                List.of(
                        column("project_tasks", "parent_task_id"),
                        column("recurring_task_definitions", "next_occurrence_at"),
                        column("recurring_task_definitions", "status"),
                        column("recurring_task_definitions", "version"),
                        column("recurring_task_occurrences", "definition_id"),
                        column("recurring_task_occurrences", "task_id"),
                        column("project_task_templates", "normalized_name"),
                        column("project_task_templates", "due_offset_minutes"),
                        column("project_templates", "normalized_name"),
                        column("project_templates", "project_name_seed"),
                        column("project_templates", "initial_status"),
                        column("project_template_tasks", "template_id"),
                        column("project_template_tasks", "position_index"),
                        column("project_template_tasks", "due_offset_minutes"),
                        column("workflow_definitions", "normalized_name"),
                        column("workflow_definitions", "status"),
                        column("workflow_definitions", "definition_version"),
                        column("workflow_nodes", "node_key"),
                        column("workflow_nodes", "node_type"),
                        column("workflow_nodes", "operation_type"),
                        column("workflow_nodes", "configuration_json"),
                        column("workflow_nodes", "position_x"),
                        column("workflow_nodes", "position_y"),
                        column("workflow_edges", "source_node_key"),
                        column("workflow_edges", "target_node_key"),
                        column("workflow_edges", "branch_type"),
                        column("workflow_executions", "workflow_version"),
                        column("workflow_executions", "event_key"),
                        column("workflow_executions", "trigger_operation"),
                        column("workflow_executions", "status"),
                        column("whiteboards", "normalized_name"),
                        column("whiteboards", "version"),
                        column("whiteboard_nodes", "node_key"),
                        column("whiteboard_nodes", "node_type"),
                        column("whiteboard_nodes", "linked_task_id"),
                        column("whiteboard_nodes", "position_x"),
                        column("whiteboard_nodes", "position_y"),
                        column("whiteboard_nodes", "z_index"),
                        column("whiteboard_edges", "source_node_key"),
                        column("whiteboard_edges", "target_node_key"),
                        column("form_definitions", "project_id"),
                        column("form_definitions", "workflow_id"),
                        column("form_definitions", "status"),
                        column("form_definitions", "definition_version"),
                        column("form_definitions", "task_title_field_key"),
                        column("form_definitions", "task_priority"),
                        column("form_fields", "form_id"),
                        column("form_fields", "field_key"),
                        column("form_fields", "field_type"),
                        column("form_fields", "options_json"),
                        column("form_fields", "position_index"),
                        column("form_submissions", "form_id"),
                        column("form_submissions", "definition_version"),
                        column("form_submissions", "payload_json"),
                        column("form_submissions", "created_task_id"),
                        column("form_submissions", "validation_context"),
                        column("approval_definitions", "project_id"),
                        column("approval_definitions", "status"),
                        column("approval_definitions", "definition_version"),
                        column("approval_stages", "definition_id"),
                        column("approval_stages", "stage_key"),
                        column("approval_stages", "position_index"),
                        column("approval_stages", "allow_requester_approval"),
                        column("approval_stage_reviewers", "stage_id"),
                        column("approval_stage_reviewers", "reviewer_user_id"),
                        column("approval_requests", "definition_version"),
                        column("approval_requests", "workflow_execution_id"),
                        column("approval_requests", "workflow_node_key"),
                        column("approval_requests", "task_id"),
                        column("approval_requests", "status"),
                        column("approval_requests", "current_stage_index"),
                        column("approval_requests", "row_version"),
                        column("approval_request_stages", "request_id"),
                        column("approval_request_stages", "stage_key"),
                        column("approval_request_stages", "status"),
                        column("approval_request_stages", "decided_by_user_id"),
                        column("approval_request_stages", "decision_comment"),
                        column("approval_request_stage_reviewers", "request_stage_id"),
                        column("approval_request_stage_reviewers", "reviewer_user_id"),
                        column("external_access_grants", "tenant_id"),
                        column("external_access_grants", "project_id"),
                        column("external_access_grants", "invitation_token_hash"),
                        column("external_access_grants", "expires_at"),
                        column("external_access_grants", "accepted_at"),
                        column("external_access_grants", "revoked_at"),
                        column("external_access_grants", "row_version"),
                        column("external_access_grant_capabilities", "grant_id"),
                        column("external_access_grant_capabilities", "capability"),
                        column("external_guest_sessions", "grant_id"),
                        column("external_guest_sessions", "token_hash"),
                        column("external_guest_sessions", "expires_at"),
                        column("external_guest_sessions", "revoked_at"),
                        column("external_guest_sessions", "row_version"),
                        column("authorization_delegations", "tenant_id"),
                        column("authorization_delegations", "delegator_user_id"),
                        column("authorization_delegations", "delegate_user_id"),
                        column("authorization_delegations", "parent_authority_assignment_id"),
                        column("authorization_delegations", "delegated_assignment_id"),
                        column("authorization_delegations", "status"),
                        column("authorization_delegations", "created_at"),
                        column("authorization_delegations", "revoked_at"),
                        column("authorization_delegations", "revoked_by_user_id"),
                        column("subscription_plan_usage_limits", "plan_id"),
                        column("subscription_plan_usage_limits", "metric_code"),
                        column("subscription_plan_usage_limits", "period_limit"),
                        column("subscription_plan_provider_mappings", "plan_id"),
                        column("subscription_plan_provider_mappings", "provider"),
                        column("subscription_plan_provider_mappings", "environment"),
                        column("subscription_plan_provider_mappings", "provider_product_id"),
                        column("subscription_plan_provider_mappings", "provider_price_id"),
                        column("subscription_plan_provider_mappings", "provider_plan_id"),
                        column("subscription_plan_provider_mappings", "status"),
                        column("subscription_plan_provider_mappings", "archived_at"),
                        column("subscription_plan_retirement_operations", "plan_id"),
                        column("subscription_plan_retirement_operations", "status"),
                        column("subscription_plan_retirement_operations", "attempt_count"),
                        column("subscription_plan_retirement_operations", "last_error"),
                        column("subscription_plan_retirement_operations", "requested_at"),
                        column("subscription_plan_retirement_operations", "started_at"),
                        column("subscription_plan_retirement_operations", "completed_at"),
                        column("subscription_plan_retirement_operations", "updated_at"),
                        column("tenant_subscription_history", "subscription_id"),
                        column("tenant_subscription_history", "tenant_id"),
                        column("tenant_subscription_history", "tenant_name_snapshot"),
                        column("tenant_subscription_history", "plan_id"),
                        column("tenant_subscription_history", "plan_code_snapshot"),
                        column("tenant_subscription_history", "plan_name_snapshot"),
                        column("tenant_subscription_history", "price_snapshot"),
                        column("tenant_subscription_history", "currency_snapshot"),
                        column("tenant_subscription_history", "status"),
                        column("tenant_subscription_history", "billing_provider"),
                        column("tenant_subscription_history", "provider_subscription_id"),
                        column("tenant_subscription_history", "event_type"),
                        column("tenant_subscription_history", "recorded_at"),
                        column("tenant_api_keys", "tenant_id"),
                        column("tenant_api_keys", "key_prefix"),
                        column("tenant_api_keys", "key_hash"),
                        column("tenant_api_keys", "created_by_user_id"),
                        column("tenant_api_keys", "last_used_at"),
                        column("tenant_api_keys", "revoked_at"),
                        column("billing_usage_events", "tenant_id"),
                        column("billing_usage_events", "metric_code"),
                        column("billing_usage_events", "idempotency_key"),
                        column("billing_usage_events", "occurred_at"),
                        column("tenant_subscriptions", "billing_provider"),
                        column("tenant_subscriptions", "provider_subscription_id"),
                        column("tenant_subscriptions", "provider_event_created_at"),
                        column("tenant_subscriptions", "plan_code_snapshot"),
                        column("tenant_subscriptions", "plan_name_snapshot"),
                        column("tenant_subscriptions", "plan_description_snapshot"),
                        column("tenant_subscriptions", "billing_interval_snapshot"),
                        column("tenant_subscriptions", "price_snapshot"),
                        column("tenant_subscriptions", "currency_snapshot"),
                        column("tenant_subscriptions", "max_users_snapshot"),
                        column("tenant_subscriptions", "max_projects_snapshot"),
                        column("tenant_subscriptions", "max_storage_mb_snapshot"),
                        column("task_attachments", "storage_deleted_at"),
                        column("task_comments", "parent_comment_id"),
                        column("task_comments", "reply_count"),
                        column("task_comments", "pinned_at"),
                        column("task_comments", "pinned_by_user_id"),
                        column("notifications", "recipient_user_id"),
                        column("notifications", "target_url"),
                        column("notifications", "read_at"),
                        column("notification_deliveries", "lease_token"),
                        column("notification_deliveries", "next_attempt_at"),
                        column("notification_deliveries", "attempt_count"),
                        column("notification_preferences", "recipient_user_id"),
                        column("notification_preferences", "type"),
                        column("notification_preferences", "email_enabled"),
                        column("outbound_webhook_endpoints", "tenant_id"),
                        column("outbound_webhook_endpoints", "url"),
                        column("outbound_webhook_endpoints", "enabled"),
                        column("outbound_webhook_endpoints", "secret_ciphertext"),
                        column("outbound_webhook_endpoints", "secret_hint"),
                        column("outbound_webhook_endpoints", "secret_version"),
                        column("outbound_webhook_endpoints", "secret_rotated_at"),
                        column("outbound_webhook_endpoints", "archived_at"),
                        column("outbound_webhook_endpoint_events", "endpoint_id"),
                        column("outbound_webhook_endpoint_events", "event_type"),
                        column("outbound_webhook_events", "tenant_id"),
                        column("outbound_webhook_events", "event_type"),
                        column("outbound_webhook_events", "payload_json"),
                        column("outbound_webhook_events", "occurred_at"),
                        column("outbound_webhook_deliveries", "event_id"),
                        column("outbound_webhook_deliveries", "endpoint_id"),
                        column("outbound_webhook_deliveries", "status"),
                        column("outbound_webhook_deliveries", "attempt_count"),
                        column("outbound_webhook_deliveries", "replay_count"),
                        column("outbound_webhook_deliveries", "next_attempt_at"),
                        column("outbound_webhook_deliveries", "processing_started_at"),
                        column("outbound_webhook_deliveries", "lease_token"),
                        column("outbound_webhook_deliveries", "last_http_status"),
                        column("outbound_webhook_delivery_attempts", "tenant_id"),
                        column("outbound_webhook_delivery_attempts", "delivery_id"),
                        column("outbound_webhook_delivery_attempts", "replay_number"),
                        column("outbound_webhook_delivery_attempts", "attempt_number"),
                        column("outbound_webhook_delivery_attempts", "lease_token"),
                        column("outbound_webhook_delivery_attempts", "outcome"),
                        column("outbound_webhook_delivery_attempts", "http_status"),
                        column("outbound_webhook_delivery_attempts", "error"),
                        column("outbound_webhook_delivery_attempts", "started_at"),
                        column("outbound_webhook_delivery_attempts", "completed_at"),
                        column("tenant_identity_providers", "tenant_id"),
                        column("tenant_identity_providers", "protocol"),
                        column("tenant_identity_providers", "display_name"),
                        column("tenant_identity_providers", "issuer_uri"),
                        column("tenant_identity_providers", "client_id"),
                        column("tenant_identity_providers", "client_secret_ciphertext"),
                        column("tenant_identity_providers", "client_secret_hint"),
                        column("tenant_identity_providers", "secret_version"),
                        column("tenant_identity_providers", "status"),
                        column("tenant_identity_providers", "sso_mode"),
                        column("tenant_identity_providers", "verified_at"),
                        column("tenant_identity_providers", "disabled_at"),
                        column("tenant_identity_providers", "created_by_user_id"),
                        column("tenant_identity_providers", "updated_by_user_id"),
                        column("tenant_identity_providers", "secret_rotated_at"),
                        column("tenant_identity_provider_scopes", "identity_provider_id"),
                        column("tenant_identity_provider_scopes", "scope"),
                        column("oidc_authorization_transactions", "tenant_id"),
                        column("oidc_authorization_transactions", "identity_provider_id"),
                        column("oidc_authorization_transactions", "identity_provider_version"),
                        column("oidc_authorization_transactions", "state_hash"),
                        column("oidc_authorization_transactions", "nonce_hash"),
                        column("oidc_authorization_transactions", "pkce_verifier_ciphertext"),
                        column("oidc_authorization_transactions", "persistent_session"),
                        column("oidc_authorization_transactions", "created_at"),
                        column("oidc_authorization_transactions", "expires_at"),
                        column("oidc_authorization_transactions", "consumed_at"),
                        column("tenant_federated_identities", "tenant_id"),
                        column("tenant_federated_identities", "identity_provider_id"),
                        column("tenant_federated_identities", "user_id"),
                        column("tenant_federated_identities", "issuer"),
                        column("tenant_federated_identities", "issuer_hash"),
                        column("tenant_federated_identities", "subject"),
                        column("tenant_federated_identities", "email_at_link"),
                        column("tenant_federated_identities", "linked_at"),
                        column("tenant_federated_identities", "last_login_at"),
                        column("oidc_session_handoffs", "code_hash"),
                        column("oidc_session_handoffs", "tenant_id"),
                        column("oidc_session_handoffs", "user_id"),
                        column("oidc_session_handoffs", "persistent_session"),
                        column("oidc_session_handoffs", "created_at"),
                        column("oidc_session_handoffs", "expires_at"),
                        column("oidc_session_handoffs", "consumed_at"));

        requiredColumns.forEach(pair -> assertColumnExists(pair[0], pair[1]));
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

    private String[] column(String tableName, String columnName) {
        return new String[] {tableName, columnName};
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
