package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.dto.BillingCheckoutConfigurationResponse;
import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import com.chacha.multitenantsaas.service.SubscriptionPlanService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingCheckoutServiceTest {

    @Test
    void exposesOnlyPaidActivePlansAndConfiguredProviders() {
        BillingProvider razorpay = mock(BillingProvider.class);
        BillingProvider stripe = mock(BillingProvider.class);
        when(razorpay.providerType()).thenReturn(BillingProviderType.RAZORPAY);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);

        SubscriptionPlanResponse free = plan("FREE", SubscriptionPlanStatus.ACTIVE, "0.00");
        SubscriptionPlanResponse pro = plan("PRO", SubscriptionPlanStatus.ACTIVE, "29.00");
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        when(planService.getPlans(true)).thenReturn(List.of(free, pro));

        BillingCheckoutService service =
                service(new BillingProviderRegistry(List.of(razorpay, stripe)), planService);

        BillingCheckoutConfigurationResponse configuration = service.getCheckoutConfiguration();

        assertThat(configuration.plans()).containsExactly(pro);
        assertThat(configuration.providers())
                .containsExactly(BillingProviderType.STRIPE, BillingProviderType.RAZORPAY);
    }

    @Test
    void reportsNoCheckoutProvidersWhenBillingIsDisabled() {
        SubscriptionPlanResponse pro = plan("PRO", SubscriptionPlanStatus.ACTIVE, "29.00");
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        when(planService.getPlans(true)).thenReturn(List.of(pro));

        BillingCheckoutService service =
                service(new BillingProviderRegistry(List.of()), planService);

        BillingCheckoutConfigurationResponse configuration = service.getCheckoutConfiguration();

        assertThat(configuration.plans()).containsExactly(pro);
        assertThat(configuration.providers()).isEmpty();
    }

    @Test
    void delegatesEligibleCheckoutToSelectedProvider() {
        UUID tenantId = UUID.randomUUID();
        BillingProvider stripe = mock(BillingProvider.class);
        BillingCheckoutSession expected =
                new BillingCheckoutSession(
                        "session-1",
                        "https://checkout.example/session-1",
                        BillingProviderType.STRIPE);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        when(stripe.createCheckoutSession(tenantId, "PRO")).thenReturn(expected);
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        stubPlan(planService, " PRO ", "PRO", SubscriptionPlanStatus.ACTIVE, "29.00");

        BillingCheckoutService service =
                service(new BillingProviderRegistry(List.of(stripe)), planService);

        BillingCheckoutSession actual =
                service.createCheckoutSession(tenantId, " PRO ", BillingProviderType.STRIPE);

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.provider()).isEqualTo(BillingProviderType.STRIPE);
        verify(stripe).createCheckoutSession(tenantId, "PRO");
    }

    @Test
    void rejectsCheckoutWhenWorkspaceAlreadyHasActiveSubscription() {
        UUID tenantId = UUID.randomUUID();
        BillingProvider razorpay = mock(BillingProvider.class);
        when(razorpay.providerType()).thenReturn(BillingProviderType.RAZORPAY);
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        stubPlan(planService, "PRO", "PRO", SubscriptionPlanStatus.ACTIVE, "29.00");

        TenantSubscription subscription = mock(TenantSubscription.class);
        when(subscription.getStatus()).thenReturn(TenantSubscriptionStatus.ACTIVE);
        TenantSubscriptionRepository subscriptionRepository =
                mock(TenantSubscriptionRepository.class);
        when(subscriptionRepository.findByTenantIdWithPlan(tenantId))
                .thenReturn(Optional.of(subscription));

        BillingCheckoutService checkoutService =
                new BillingCheckoutService(
                        new BillingProviderRegistry(List.of(razorpay)),
                        planService,
                        subscriptionRepository);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                checkoutService.createCheckoutSession(
                                        tenantId, "PRO", BillingProviderType.RAZORPAY))
                .withMessage("Workspace already has an active subscription");
    }

    @Test
    void rejectsUnconfiguredProvider() {
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        stubPlan(planService, "PRO", "PRO", SubscriptionPlanStatus.ACTIVE, "29.00");
        BillingCheckoutService service =
                service(new BillingProviderRegistry(List.of()), planService);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                service.createCheckoutSession(
                                        UUID.randomUUID(), "PRO", BillingProviderType.RAZORPAY))
                .withMessage("Billing provider is not configured: RAZORPAY");
    }

    @Test
    void rejectsInactivePlan() {
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        stubPlan(planService, "PRO", "PRO", SubscriptionPlanStatus.INACTIVE, "29.00");
        BillingCheckoutService service =
                service(new BillingProviderRegistry(List.of()), planService);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                service.createCheckoutSession(
                                        UUID.randomUUID(), "PRO", BillingProviderType.STRIPE))
                .withMessage("Subscription plan must be active");
    }

    @Test
    void rejectsFreePlan() {
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        stubPlan(planService, "FREE", "FREE", SubscriptionPlanStatus.ACTIVE, "0.00");
        BillingCheckoutService service =
                service(new BillingProviderRegistry(List.of()), planService);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                service.createCheckoutSession(
                                        UUID.randomUUID(), "FREE", BillingProviderType.STRIPE))
                .withMessage("Free plans do not require provider checkout");
    }

    @Test
    void rejectsDuplicateProviderRegistration() {
        BillingProvider first = mock(BillingProvider.class);
        BillingProvider second = mock(BillingProvider.class);
        when(first.providerType()).thenReturn(BillingProviderType.STRIPE);
        when(second.providerType()).thenReturn(BillingProviderType.STRIPE);

        assertThatIllegalStateException()
                .isThrownBy(() -> new BillingProviderRegistry(List.of(first, second)))
                .withMessage("Multiple billing providers registered for STRIPE");
    }

    @Test
    void rejectsBlankPlanCodeBeforePlanLookup() {
        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        SubscriptionPlanService planService = mock(SubscriptionPlanService.class);
        BillingCheckoutService service =
                service(new BillingProviderRegistry(List.of(stripe)), planService);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                service.createCheckoutSession(
                                        UUID.randomUUID(), " ", BillingProviderType.STRIPE))
                .withMessage("planCode must not be blank");
    }

    @Test
    void rejectsNullTenantId() {
        BillingCheckoutService service =
                service(
                        new BillingProviderRegistry(List.of()),
                        mock(SubscriptionPlanService.class));

        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                service.createCheckoutSession(
                                        null, "PRO", BillingProviderType.STRIPE))
                .withMessage("tenantId must not be null");
    }

    @Test
    void rejectsNullProviderType() {
        BillingCheckoutService service =
                service(
                        new BillingProviderRegistry(List.of()),
                        mock(SubscriptionPlanService.class));

        assertThatNullPointerException()
                .isThrownBy(() -> service.createCheckoutSession(UUID.randomUUID(), "PRO", null))
                .withMessage("providerType must not be null");
    }

    private BillingCheckoutService service(
            BillingProviderRegistry providerRegistry,
            SubscriptionPlanService subscriptionPlanService) {
        return new BillingCheckoutService(
                providerRegistry,
                subscriptionPlanService,
                mock(TenantSubscriptionRepository.class));
    }

    private void stubPlan(
            SubscriptionPlanService planService,
            String requestedCode,
            String responseCode,
            SubscriptionPlanStatus status,
            String price) {
        SubscriptionPlanResponse plan = plan(responseCode, status, price);
        when(planService.getPlanByCode(requestedCode)).thenReturn(plan);
    }

    private SubscriptionPlanResponse plan(
            String code, SubscriptionPlanStatus status, String price) {
        SubscriptionPlanResponse plan = mock(SubscriptionPlanResponse.class);
        when(plan.code()).thenReturn(code);
        when(plan.status()).thenReturn(status);
        when(plan.price()).thenReturn(new BigDecimal(price));
        return plan;
    }
}
