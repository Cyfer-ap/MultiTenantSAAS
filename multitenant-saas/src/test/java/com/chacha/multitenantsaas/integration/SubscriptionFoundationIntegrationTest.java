package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.SubscriptionPlanCreateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionLifecycleUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionPlanChangeRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.entity.BillingInterval;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import com.chacha.multitenantsaas.service.TenantSubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubscriptionFoundationIntegrationTest {

    @Autowired
    private SubscriptionPlanService subscriptionPlanService;

    @Autowired
    private TenantSubscriptionService tenantSubscriptionService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void createsAndNormalizesSubscriptionPlan() {
        SubscriptionPlanResponse plan =
                createPlan("starter-plan", "Starter", "usd");

        assertEquals("STARTER_PLAN", plan.code());
        assertEquals("USD", plan.currency());
        assertEquals(new BigDecimal("19.00"), plan.price());
        assertEquals(SubscriptionPlanStatus.ACTIVE, plan.status());
        assertEquals(10, plan.maxUsers());
        assertNotNull(plan.createdAt());

        assertThrows(
                DuplicateResourceException.class,
                () -> createPlan(
                        "starter_plan",
                        "Duplicate Starter",
                        "USD"
                )
        );
    }

    @Test
    void requiresAnActivePlanAndOneSubscriptionPerTenant() {
        Tenant tenant = createTenant("Acme Labs");
        SubscriptionPlanResponse plan =
                createPlan("trial", "Trial", "USD");

        subscriptionPlanService.changeStatus(
                plan.id(),
                SubscriptionPlanStatus.INACTIVE
        );

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        TenantSubscriptionStartRequest request =
                new TenantSubscriptionStartRequest(
                        plan.id(),
                        TenantSubscriptionStatus.TRIALING,
                        now,
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        now.plus(14, ChronoUnit.DAYS),
                        false
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> tenantSubscriptionService
                        .startSubscription(tenant.getId(), request)
        );

        subscriptionPlanService.changeStatus(
                plan.id(),
                SubscriptionPlanStatus.ACTIVE
        );

        TenantSubscriptionResponse subscription =
                tenantSubscriptionService.startSubscription(
                        tenant.getId(),
                        request
                );

        assertEquals(
                TenantSubscriptionStatus.TRIALING,
                subscription.status()
        );
        assertEquals("TRIAL", subscription.plan().code());
        assertFalse(subscription.cancelAtPeriodEnd());

        assertThrows(
                DuplicateResourceException.class,
                () -> tenantSubscriptionService
                        .startSubscription(tenant.getId(), request)
        );
    }

    @Test
    void changesPlanAndUpdatesSubscriptionLifecycle() {
        Tenant tenant = createTenant("Orbit Systems");
        SubscriptionPlanResponse monthly =
                createPlan("monthly", "Monthly", "USD");
        SubscriptionPlanResponse annual =
                createPlan("annual", "Annual", "USD");

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        tenantSubscriptionService.startSubscription(
                tenant.getId(),
                new TenantSubscriptionStartRequest(
                        monthly.id(),
                        TenantSubscriptionStatus.ACTIVE,
                        now,
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        null,
                        false
                )
        );

        TenantSubscriptionResponse changed =
                tenantSubscriptionService.changePlan(
                        tenant.getId(),
                        new TenantSubscriptionPlanChangeRequest(
                                annual.id(),
                                now.plus(30, ChronoUnit.DAYS),
                                now.plus(395, ChronoUnit.DAYS)
                        )
                );

        assertEquals("ANNUAL", changed.plan().code());

        TenantSubscriptionResponse cancelled =
                tenantSubscriptionService.updateLifecycle(
                        tenant.getId(),
                        new TenantSubscriptionLifecycleUpdateRequest(
                                TenantSubscriptionStatus.CANCELLED,
                                true,
                                null,
                                null
                        )
                );

        assertEquals(
                TenantSubscriptionStatus.CANCELLED,
                cancelled.status()
        );
        assertFalse(cancelled.cancelAtPeriodEnd());
        assertNotNull(cancelled.cancelledAt());

        assertThrows(
                IllegalArgumentException.class,
                () -> tenantSubscriptionService.changePlan(
                        tenant.getId(),
                        new TenantSubscriptionPlanChangeRequest(
                                monthly.id(),
                                now,
                                now.plus(30, ChronoUnit.DAYS)
                        )
                )
        );
    }

    @Test
    void supportsScheduledCancellationWithoutImmediateCancellation() {
        Tenant tenant = createTenant("Scheduled Labs");
        SubscriptionPlanResponse plan =
                createPlan("growth", "Growth", "USD");
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        tenantSubscriptionService.startSubscription(
                tenant.getId(),
                new TenantSubscriptionStartRequest(
                        plan.id(),
                        TenantSubscriptionStatus.ACTIVE,
                        now,
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        null,
                        false
                )
        );

        TenantSubscriptionResponse updated =
                tenantSubscriptionService.updateLifecycle(
                        tenant.getId(),
                        new TenantSubscriptionLifecycleUpdateRequest(
                                TenantSubscriptionStatus.ACTIVE,
                                true,
                                null,
                                null
                        )
                );

        assertTrue(updated.cancelAtPeriodEnd());
        assertNull(updated.cancelledAt());
    }

    private SubscriptionPlanResponse createPlan(
            String code,
            String name,
            String currency
    ) {
        return subscriptionPlanService.createPlan(
                new SubscriptionPlanCreateRequest(
                        code,
                        name,
                        name + " plan",
                        BillingInterval.MONTHLY,
                        new BigDecimal("19"),
                        currency,
                        10,
                        25,
                        1024L
                )
        );
    }

    private Tenant createTenant(String name) {
        String suffix = UUID.randomUUID()
                .toString()
                .substring(0, 8);

        return tenantRepository.saveAndFlush(
                new Tenant(
                        name,
                        name.toLowerCase()
                                .replace(' ', '-')
                                + "-"
                                + suffix
                )
        );
    }
}
