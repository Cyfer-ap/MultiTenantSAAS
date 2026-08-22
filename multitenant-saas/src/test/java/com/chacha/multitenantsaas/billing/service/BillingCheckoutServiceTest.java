package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.provider.BillingCheckoutSession;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingCheckoutServiceTest {

    @Test
    void delegatesCheckoutToSelectedProvider() {
        UUID tenantId = UUID.randomUUID();
        BillingProvider stripe = mock(BillingProvider.class);
        BillingCheckoutSession expected =
                new BillingCheckoutSession(
                        "session-1",
                        "https://checkout.example/session-1",
                        BillingProviderType.STRIPE);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        when(stripe.createCheckoutSession(tenantId, "PRO")).thenReturn(expected);

        BillingCheckoutService service =
                new BillingCheckoutService(new BillingProviderRegistry(List.of(stripe)));

        BillingCheckoutSession actual =
                service.createCheckoutSession(tenantId, " PRO ", BillingProviderType.STRIPE);

        assertThat(actual).isEqualTo(expected);
        verify(stripe).createCheckoutSession(tenantId, "PRO");
    }

    @Test
    void rejectsUnconfiguredProvider() {
        BillingCheckoutService service =
                new BillingCheckoutService(new BillingProviderRegistry(List.of()));

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                service.createCheckoutSession(
                                        UUID.randomUUID(), "PRO", BillingProviderType.RAZORPAY))
                .withMessage("Billing provider is not configured: RAZORPAY");
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
    void rejectsBlankPlanCodeBeforeProviderInvocation() {
        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        BillingCheckoutService service =
                new BillingCheckoutService(new BillingProviderRegistry(List.of(stripe)));

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
                new BillingCheckoutService(new BillingProviderRegistry(List.of()));

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
                new BillingCheckoutService(new BillingProviderRegistry(List.of()));

        assertThatNullPointerException()
                .isThrownBy(() -> service.createCheckoutSession(UUID.randomUUID(), "PRO", null))
                .withMessage("providerType must not be null");
    }
}
