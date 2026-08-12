package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.dto.ForgotPasswordRequest;
import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.dto.ResetPasswordRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanCreateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.dto.UserInvitationAcceptRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.entity.UserInvitation;
import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException.RestrictionType;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.UserInvitationRepository;
import com.chacha.multitenantsaas.service.PasswordResetService;
import com.chacha.multitenantsaas.service.SecureTokenService;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import com.chacha.multitenantsaas.service.TenantSubscriptionService;
import com.chacha.multitenantsaas.service.UserInvitationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class PostgreSqlConcurrencyCloseoutIntegrationTest {

    private static final String ADMIN_PASSWORD = "TenantAdmin@123";

    private static final String INVITED_PASSWORD = "InvitedUser@123";

    private static final String RESET_PASSWORD = "ResetPassword@456";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("multitenant_saas_concurrency_closeout")
                    .withUsername("saas")
                    .withPassword("saas");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "8");
        registry.add(
                "spring.flyway.locations", () -> "classpath:db/postgresql,classpath:db/common");
        registry.add("app.subscription.enforcement.enabled", () -> "true");
    }

    @Autowired private TenantOnboardingService tenantOnboardingService;

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private SubscriptionPlanService subscriptionPlanService;

    @Autowired private TenantSubscriptionService tenantSubscriptionService;

    @Autowired private SecureTokenService secureTokenService;

    @Autowired private UserInvitationRepository userInvitationRepository;

    @Autowired private UserInvitationService userInvitationService;

    @Autowired private PasswordResetService passwordResetService;

    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void concurrentInvitationAcceptancesCannotExceedFinalUserSlot() throws Exception {
        TenantOnboardingResponse onboarding = onboardTenant("quota-race");

        UUID tenantId = onboarding.tenant().id();

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();

        AppUser administrator =
                appUserRepository.findById(onboarding.adminUser().id()).orElseThrow();

        SubscriptionPlanResponse plan =
                subscriptionPlanService.createPlan(
                        new SubscriptionPlanCreateRequest(
                                "quota-race-" + shortId(),
                                "Quota Race Plan",
                                "Plan with one remaining user slot",
                                BillingInterval.MONTHLY,
                                new BigDecimal("19.00"),
                                "USD",
                                2,
                                25,
                                1024L));

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        tenantSubscriptionService.startSubscription(
                tenantId,
                new TenantSubscriptionStartRequest(
                        plan.id(),
                        TenantSubscriptionStatus.ACTIVE,
                        now,
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        null,
                        false));

        InvitationFixture firstInvitation =
                createInvitation(tenant, administrator, "first-" + shortId() + "@example.test");

        InvitationFixture secondInvitation =
                createInvitation(tenant, administrator, "second-" + shortId() + "@example.test");

        List<Throwable> outcomes =
                runConcurrently(
                        () ->
                                userInvitationService.acceptInvitation(
                                        new UserInvitationAcceptRequest(
                                                firstInvitation.rawToken(),
                                                INVITED_PASSWORD,
                                                INVITED_PASSWORD)),
                        () ->
                                userInvitationService.acceptInvitation(
                                        new UserInvitationAcceptRequest(
                                                secondInvitation.rawToken(),
                                                INVITED_PASSWORD,
                                                INVITED_PASSWORD)));

        assertThat(successCount(outcomes)).isEqualTo(1);
        assertThat(errorCount(outcomes, SubscriptionRestrictionException.class)).isEqualTo(1);

        SubscriptionRestrictionException restriction =
                outcomes.stream()
                        .filter(SubscriptionRestrictionException.class::isInstance)
                        .map(SubscriptionRestrictionException.class::cast)
                        .findFirst()
                        .orElseThrow();

        assertThat(restriction.getRestrictionType()).isEqualTo(RestrictionType.USER_LIMIT_REACHED);

        assertThat(appUserRepository.countByTenantId(tenantId)).isEqualTo(2);

        List<UserInvitationStatus> statuses =
                List.of(
                        userInvitationRepository
                                .findByTenant_IdAndId(tenantId, firstInvitation.invitationId())
                                .orElseThrow()
                                .getStatus(),
                        userInvitationRepository
                                .findByTenant_IdAndId(tenantId, secondInvitation.invitationId())
                                .orElseThrow()
                                .getStatus());

        assertThat(statuses)
                .containsExactlyInAnyOrder(
                        UserInvitationStatus.ACCEPTED, UserInvitationStatus.PENDING);
    }

    @Test
    void concurrentPasswordResetConsumesTokenOnlyOnce() throws Exception {
        TenantOnboardingResponse onboarding = onboardTenant("reset-race");

        UUID tenantId = onboarding.tenant().id();
        UUID userId = onboarding.adminUser().id();

        ForgotPasswordResponse forgotPassword =
                passwordResetService.forgotPassword(
                        tenantId, new ForgotPasswordRequest(onboarding.adminUser().email()));

        ResetPasswordRequest request =
                new ResetPasswordRequest(
                        forgotPassword.devResetToken(), RESET_PASSWORD, RESET_PASSWORD);

        List<Throwable> outcomes =
                runConcurrently(
                        () -> passwordResetService.resetPassword(request),
                        () -> passwordResetService.resetPassword(request));

        assertThat(successCount(outcomes)).isEqualTo(1);
        assertThat(errorCount(outcomes, AuthenticationFailedException.class)).isEqualTo(1);

        AppUser reloadedUser = appUserRepository.findById(userId).orElseThrow();

        assertThat(reloadedUser.getSessionVersion()).isEqualTo(1L);

        assertThat(passwordEncoder.matches(RESET_PASSWORD, reloadedUser.getPasswordHash()))
                .isTrue();
    }

    private TenantOnboardingResponse onboardTenant(String prefix) {
        String suffix = shortId();

        return tenantOnboardingService.onboardTenant(
                new TenantOnboardingRequest(
                        prefix + " Tenant",
                        prefix + "-" + suffix,
                        "Concurrency Administrator",
                        "admin-" + suffix + "@example.test",
                        ADMIN_PASSWORD));
    }

    private InvitationFixture createInvitation(Tenant tenant, AppUser invitedBy, String email) {
        String rawToken = secureTokenService.generateToken();

        UserInvitation invitation =
                new UserInvitation(
                        tenant,
                        invitedBy,
                        null,
                        "Concurrency Invitee",
                        email,
                        UserRole.TENANT_USER,
                        secureTokenService.hashToken(rawToken),
                        Instant.now().plus(1, ChronoUnit.DAYS));

        UserInvitation saved = userInvitationRepository.saveAndFlush(invitation);

        return new InvitationFixture(saved.getId(), rawToken);
    }

    private List<Throwable> runConcurrently(
            ThrowingRunnable firstOperation, ThrowingRunnable secondOperation) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CyclicBarrier startBarrier = new CyclicBarrier(2);

        try {
            Callable<Throwable> first = () -> runAfterBarrier(startBarrier, firstOperation);

            Callable<Throwable> second = () -> runAfterBarrier(startBarrier, secondOperation);

            Future<Throwable> firstFuture = executor.submit(first);

            Future<Throwable> secondFuture = executor.submit(second);

            return Arrays.asList(
                    firstFuture.get(15, TimeUnit.SECONDS), secondFuture.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private Throwable runAfterBarrier(CyclicBarrier startBarrier, ThrowingRunnable operation)
            throws Exception {
        startBarrier.await(5, TimeUnit.SECONDS);

        try {
            operation.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private long successCount(List<Throwable> outcomes) {
        return outcomes.stream().filter(Objects::isNull).count();
    }

    private long errorCount(List<Throwable> outcomes, Class<? extends Throwable> errorType) {
        return outcomes.stream().filter(errorType::isInstance).count();
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record InvitationFixture(UUID invitationId, String rawToken) {}

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Exception;
    }
}
