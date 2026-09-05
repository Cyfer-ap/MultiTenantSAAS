package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderException;
import com.chacha.multitenantsaas.billing.provider.BillingProviderSubscriptionSnapshot;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BillingCancellationServiceTest {

    @Test
    void delegatesLinkedSubscriptionCancellationToRecordedProvider() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription =
                subscription(
                        TenantSubscriptionStatus.ACTIVE, BillingProviderType.STRIPE, "sub_123");
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));

        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of(stripe)), repository);

        BillingCancellationResult result = service.requestCancellation(tenantId);

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.provider()).isEqualTo(BillingProviderType.STRIPE);
        assertThat(result.providerSubscriptionId()).isEqualTo("sub_123");
        verify(stripe).cancelSubscription("sub_123");
        verify(repository, never()).save(subscription);
    }

    @Test
    void repairsStaleProviderLinkageAndCancelsThroughVerifiedOwner() {
        UUID tenantId = UUID.randomUUID();
        String subscriptionId = "sub_razorpay_123";
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription =
                subscription(
                        TenantSubscriptionStatus.ACTIVE,
                        BillingProviderType.STRIPE,
                        subscriptionId);
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));

        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        BillingProviderException stripeFailure =
                new BillingProviderException("Stripe subscription cancellation failed", null);
        doThrow(stripeFailure).when(stripe).cancelSubscription(subscriptionId);

        BillingProvider razorpay = mock(BillingProvider.class);
        when(razorpay.providerType()).thenReturn(BillingProviderType.RAZORPAY);
        when(razorpay.fetchSubscription(subscriptionId))
                .thenReturn(
                        new BillingProviderSubscriptionSnapshot(
                                BillingProviderType.RAZORPAY,
                                subscriptionId,
                                "PRO",
                                TenantSubscriptionStatus.ACTIVE,
                                Instant.parse("2026-08-27T12:33:00Z"),
                                Instant.parse("2026-09-27T12:33:00Z"),
                                false));

        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of(stripe, razorpay)), repository);

        BillingCancellationResult result = service.requestCancellation(tenantId);

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.provider()).isEqualTo(BillingProviderType.RAZORPAY);
        assertThat(result.providerSubscriptionId()).isEqualTo(subscriptionId);
        verify(stripe).cancelSubscription(subscriptionId);
        verify(razorpay).fetchSubscription(subscriptionId);
        verify(subscription).setBillingProvider(BillingProviderType.RAZORPAY);
        verify(repository).save(subscription);
        verify(razorpay).cancelSubscription(subscriptionId);
    }

    @Test
    void preservesRecordedProviderFailureWhenNoAlternativeOwnsSubscription() {
        UUID tenantId = UUID.randomUUID();
        String subscriptionId = "sub_unknown";
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription =
                subscription(
                        TenantSubscriptionStatus.ACTIVE,
                        BillingProviderType.STRIPE,
                        subscriptionId);
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));

        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        BillingProviderException stripeFailure =
                new BillingProviderException("Stripe subscription cancellation failed", null);
        doThrow(stripeFailure).when(stripe).cancelSubscription(subscriptionId);

        BillingProvider razorpay = mock(BillingProvider.class);
        when(razorpay.providerType()).thenReturn(BillingProviderType.RAZORPAY);
        when(razorpay.fetchSubscription(subscriptionId))
                .thenThrow(new BillingProviderException("Razorpay subscription lookup failed", null));

        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of(stripe, razorpay)), repository);

        assertThatThrownBy(() -> service.requestCancellation(tenantId)).isSameAs(stripeFailure);
        verify(razorpay, never()).cancelSubscription(anyString());
        verify(repository, never()).save(subscription);
    }

    @Test
    void rejectsSubscriptionWithoutProviderLinkage() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription = subscription(TenantSubscriptionStatus.ACTIVE, null, null);
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));
        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of(stripe)), repository);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.requestCancellation(tenantId))
                .withMessage("Tenant subscription is not linked to a billing provider");
        verify(stripe, never()).cancelSubscription(anyString());
    }

    @Test
    void rejectsTerminalSubscriptionBeforeProviderCall() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription =
                subscription(
                        TenantSubscriptionStatus.CANCELLED,
                        BillingProviderType.RAZORPAY,
                        "sub_cancelled");
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));
        BillingProvider razorpay = mock(BillingProvider.class);
        when(razorpay.providerType()).thenReturn(BillingProviderType.RAZORPAY);
        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of(razorpay)), repository);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.requestCancellation(tenantId))
                .withMessage("Subscription is already terminal: CANCELLED");
        verify(razorpay, never()).cancelSubscription(anyString());
    }

    @Test
    void rejectsProviderThatIsNotConfigured() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        TenantSubscription subscription =
                subscription(
                        TenantSubscriptionStatus.ACTIVE, BillingProviderType.RAZORPAY, "sub_123");
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.of(subscription));
        BillingCancellationService service =
                new BillingCancellationService(new BillingProviderRegistry(List.of()), repository);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.requestCancellation(tenantId))
                .withMessage("Billing provider is not configured: RAZORPAY");
    }

    @Test
    void rejectsMissingTenantSubscription() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        when(repository.findByTenantIdWithPlan(tenantId)).thenReturn(Optional.empty());
        BillingCancellationService service =
                new BillingCancellationService(new BillingProviderRegistry(List.of()), repository);

        assertThatThrownBy(() -> service.requestCancellation(tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tenant subscription not found for tenant: " + tenantId);
    }

    @Test
    void rejectsNullTenantId() {
        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of()),
                        mock(TenantSubscriptionRepository.class));

        assertThatNullPointerException()
                .isThrownBy(() -> service.requestCancellation(null))
                .withMessage("tenantId must not be null");
    }

    private TenantSubscription subscription(
            TenantSubscriptionStatus status,
            BillingProviderType providerType,
            String providerSubscriptionId) {
        TenantSubscription subscription = mock(TenantSubscription.class);
        when(subscription.getStatus()).thenReturn(status);
        when(subscription.getBillingProvider()).thenReturn(providerType);
        when(subscription.getProviderSubscriptionId()).thenReturn(providerSubscriptionId);
        return subscription;
    }
}
