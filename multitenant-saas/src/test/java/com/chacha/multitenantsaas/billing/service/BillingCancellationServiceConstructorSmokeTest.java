package com.chacha.multitenantsaas.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class BillingCancellationServiceConstructorSmokeTest {

    @Test
    void constructsWithHistoryResolverDependency() {
        BillingCancellationService service =
                new BillingCancellationService(
                        new BillingProviderRegistry(List.of()),
                        mock(TenantSubscriptionRepository.class),
                        mock(BillingSubscriptionHistoryResolver.class));

        assertThat(service).isNotNull();
    }
}
