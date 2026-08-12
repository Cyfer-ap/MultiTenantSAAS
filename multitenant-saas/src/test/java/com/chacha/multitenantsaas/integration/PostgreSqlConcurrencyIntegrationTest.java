package com.chacha.multitenantsaas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.dto.LoginRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanCreateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.RefreshToken;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.RefreshTokenRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import com.chacha.multitenantsaas.service.AuthService;
import com.chacha.multitenantsaas.service.RefreshTokenService;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import com.chacha.multitenantsaas.service.TenantSubscriptionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlConcurrencyIntegrationTest {

    private static final String CORRECT_PASSWORD = "CorrectPassword@123";

    private static final String WRONG_PASSWORD = "WrongPassword@123";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("multitenant_saas_concurrency_test")
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
    }

    @Autowired private TenantRepository tenantRepository;

    @Autowired private AppUserRepository appUserRepository;

    @Autowired private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @Autowired private SubscriptionPlanService subscriptionPlanService;

    @Autowired private TenantSubscriptionService tenantSubscriptionService;

    @Autowired private AuthService authService;

    @Autowired private RefreshTokenService refreshTokenService;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void postgresWriteLockBlocksCompetingUserTransaction() throws Exception {
        AppUser user = createUser();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstLock = new CountDownLatch(1);
        CountDownLatch contenderStarted = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> lockHolder =
                    executor.submit(
                            () -> {
                                transactionTemplate.executeWithoutResult(
                                        status -> {
                                            appUserRepository
                                                    .findByIdForUpdate(user.getId())
                                                    .orElseThrow();

                                            firstLockAcquired.countDown();
                                            await(releaseFirstLock);
                                        });
                            });

            assertThat(firstLockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> contender =
                    executor.submit(
                            () -> {
                                contenderStarted.countDown();

                                transactionTemplate.executeWithoutResult(
                                        status ->
                                                appUserRepository
                                                        .findByIdForUpdate(user.getId())
                                                        .orElseThrow());
                            });

            assertThat(contenderStarted.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> contender.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            releaseFirstLock.countDown();

            lockHolder.get(5, TimeUnit.SECONDS);
            contender.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirstLock.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void concurrentSubscriptionStartCreatesExactlyOneSubscription() throws Exception {
        Tenant tenant = createTenant();

        SubscriptionPlanResponse plan =
                subscriptionPlanService.createPlan(
                        new SubscriptionPlanCreateRequest(
                                "race-plan-" + shortId(),
                                "Concurrency Plan",
                                "Concurrency integration plan",
                                BillingInterval.MONTHLY,
                                new BigDecimal("19.00"),
                                "USD",
                                10,
                                25,
                                1024L));

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        TenantSubscriptionStartRequest request =
                new TenantSubscriptionStartRequest(
                        plan.id(),
                        TenantSubscriptionStatus.ACTIVE,
                        now,
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        null,
                        false);

        List<Throwable> outcomes =
                runConcurrently(
                        () -> tenantSubscriptionService.startSubscription(tenant.getId(), request));

        assertThat(successCount(outcomes)).isEqualTo(1);
        assertThat(errorCount(outcomes, DuplicateResourceException.class)).isEqualTo(1);

        assertThat(tenantSubscriptionRepository.findByTenantIdWithPlan(tenant.getId())).isPresent();
    }

    @Test
    void concurrentFailedLoginsPreserveBothAttempts() throws Exception {
        AppUser user = createUser();

        LoginRequest request = new LoginRequest(user.getEmail(), WRONG_PASSWORD);

        List<Throwable> outcomes =
                runConcurrently(() -> authService.login(user.getTenant().getId(), request));

        assertThat(outcomes).allMatch(AuthenticationFailedException.class::isInstance);

        AppUser reloadedUser = appUserRepository.findById(user.getId()).orElseThrow();

        assertThat(reloadedUser.getFailedLoginAttempts()).isEqualTo(2);
    }

    @Test
    void concurrentRefreshRotationConsumesTokenOnlyOnce() throws Exception {
        AppUser user = createUser();

        String rawRefreshToken = refreshTokenService.createRefreshToken(user);

        List<Throwable> outcomes =
                runConcurrently(() -> refreshTokenService.rotateRefreshToken(rawRefreshToken));

        assertThat(successCount(outcomes)).isEqualTo(1);
        assertThat(errorCount(outcomes, AuthenticationFailedException.class)).isEqualTo(1);

        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(user.getId());

        long activeTokens = tokens.stream().filter(RefreshToken::isActive).count();

        long revokedTokens = tokens.stream().filter(RefreshToken::isRevoked).count();

        assertThat(tokens).hasSize(2);
        assertThat(activeTokens).isEqualTo(1);
        assertThat(revokedTokens).isEqualTo(1);
    }

    private AppUser createUser() {
        Tenant tenant = createTenant();

        AppUser user =
                new AppUser(
                        tenant,
                        "Concurrency User",
                        "concurrency-" + shortId() + "@example.test",
                        passwordEncoder.encode(CORRECT_PASSWORD),
                        UserRole.TENANT_USER);

        return appUserRepository.saveAndFlush(user);
    }

    private Tenant createTenant() {
        return tenantRepository.saveAndFlush(
                new Tenant("Concurrency Tenant", "concurrency-tenant-" + shortId()));
    }

    private List<Throwable> runConcurrently(ThrowingRunnable operation) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier startBarrier = new CyclicBarrier(2);

        try {
            Callable<Throwable> contender =
                    () -> {
                        startBarrier.await(5, TimeUnit.SECONDS);

                        try {
                            operation.run();
                            return null;
                        } catch (Throwable throwable) {
                            return throwable;
                        }
                    };

            Future<Throwable> first = executor.submit(contender);

            Future<Throwable> second = executor.submit(contender);

            return Arrays.asList(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
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

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrency latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for concurrency latch", exception);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Exception;
    }
}
