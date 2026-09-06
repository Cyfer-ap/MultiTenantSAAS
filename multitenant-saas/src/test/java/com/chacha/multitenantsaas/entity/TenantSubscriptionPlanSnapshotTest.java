package com.chacha.multitenantsaas.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantSubscriptionPlanSnapshotTest {

    @Test
    void samePlanLifecycleRefreshDoesNotRewritePurchasedTerms() {
        SubscriptionPlan plan = plan("PRO", "Pro", "499.00", 10, 20);
        Tenant tenant = new Tenant("Acme");
        Instant now = Instant.now();

        TenantSubscription subscription =
                new TenantSubscription(
                        tenant,
                        plan,
                        TenantSubscriptionStatus.ACTIVE,
                        now,
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        null,
                        false);

        plan.setName("Pro Plus");
        plan.setPrice(new BigDecimal("999.00"));
        plan.setMaxUsers(50);
        subscription.setPlan(plan);

        assertThat(subscription.getPlanNameSnapshot()).isEqualTo("Pro");
        assertThat(subscription.getPriceSnapshot()).isEqualByComparingTo("499.00");
        assertThat(subscription.getMaxUsersSnapshot()).isEqualTo(10);
    }

    @Test
    void changingPlanCapturesNewPurchasedTerms() {
        SubscriptionPlan original = plan("PRO", "Pro", "499.00", 10, 20);
        SubscriptionPlan enterprise = plan("ENTERPRISE", "Enterprise", "1499.00", 100, 200);
        Tenant tenant = new Tenant("Acme");
        Instant now = Instant.now();

        TenantSubscription subscription =
                new TenantSubscription(
                        tenant,
                        original,
                        TenantSubscriptionStatus.ACTIVE,
                        now,
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        null,
                        false);

        subscription.setPlan(enterprise);

        assertThat(subscription.getPlanCodeSnapshot()).isEqualTo("ENTERPRISE");
        assertThat(subscription.getPlanNameSnapshot()).isEqualTo("Enterprise");
        assertThat(subscription.getPriceSnapshot()).isEqualByComparingTo("1499.00");
        assertThat(subscription.getMaxUsersSnapshot()).isEqualTo(100);
        assertThat(subscription.getMaxProjectsSnapshot()).isEqualTo(200);
    }

    private SubscriptionPlan plan(
            String code, String name, String price, Integer maxUsers, Integer maxProjects) {
        SubscriptionPlan plan =
                new SubscriptionPlan(
                        code,
                        name,
                        name + " plan",
                        BillingInterval.MONTHLY,
                        new BigDecimal(price),
                        "INR",
                        maxUsers,
                        maxProjects,
                        1024L);
        plan.setId(UUID.randomUUID());
        return plan;
    }
}
