package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.billing.provider.BillingCancellationResult;
import com.chacha.multitenantsaas.billing.provider.BillingProvider;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
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
    }

    @Test
    void rejectsSubscriptionWithoutProviderLinkage() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        when(repository.findByTenantIdWithPlan(tenantId))
                .thenReturn(Optional.of(subscription(TenantSubscriptionStatus.ACTIVE, null, null)));
        BillingProvider stripe = mock(BillingProvider.class);
        when(stripe.providerType()).thenReturn(BillingProviderType.STRIPE);
        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of(stripe)), repository);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.requestCancellation(tenantId))
                .withMessage("Tenant subscription is not linked to a billing provider");
        verify(stripe, never()).cancelSubscription(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsTerminalSubscriptionBeforeProviderCall() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        when(repository.findByTenantIdWithPlan(tenantId))
                .thenReturn(
                        Optional.of(
                                subscription(
                                        TenantSubscriptionStatus.CANCELLED,
                                        BillingProviderType.RAZORPAY,
                                        "sub_cancelled")));
        BillingProvider razorpay = mock(BillingProvider.class);
        when(razorpay.providerType()).thenReturn(BillingProviderType.RAZORPAY);
        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of(razorpay)), repository);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.requestCancellation(tenantId))
                .withMessage("Subscription is already terminal: CANCELLED");
        verify(razorpay, never()).cancelSubscription(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsProviderThatIsNotConfigured() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionRepository repository = mock(TenantSubscriptionRepository.class);
        when(repository.findByTenantIdWithPlan(tenantId))
                .thenReturn(
                        Optional.of(
                                subscription(
                                        TenantSubscriptionStatus.ACTIVE,
                                        BillingProviderType.RAZORPAY,
                                        "sub_123")));
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
