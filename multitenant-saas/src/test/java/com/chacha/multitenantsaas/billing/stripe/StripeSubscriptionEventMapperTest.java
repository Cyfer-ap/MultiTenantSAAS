package com.chacha.multitenantsaas.billing.stripe;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionUpdate;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class StripeSubscriptionEventMapperTest {

    @Test
    void mapsCurrentStripeSubscriptionItemPeriodsAndMetadata() {
        UUID tenantId = UUID.randomUUID();
        String payload =
                """
                {
                  "created": 1750000300,
                  "data": {"object": {
                    "id": "sub_123",
                    "status": "active",
                    "start_date": 1750000000,
                    "cancel_at_period_end": false,
                    "metadata": {"tenant_id": "%s", "plan_code": "PRO"},
                    "items": {"data": [{
                      "current_period_start": 1750000000,
                      "current_period_end": 1752592000
                    }]}
                  }}
                }
                """
                        .formatted(tenantId);
        StripeSubscriptionEventMapper mapper =
                new StripeSubscriptionEventMapper(JsonMapper.builder().build());

        Optional<BillingSubscriptionUpdate> result =
                mapper.map(
                        new VerifiedBillingEvent(
                                BillingProviderType.STRIPE,
                                "evt_123",
                                "customer.subscription.updated",
                                payload));

        assertThat(result).isPresent();
        BillingSubscriptionUpdate update = result.orElseThrow();
        assertThat(update.tenantId()).isEqualTo(tenantId);
        assertThat(update.providerSubscriptionId()).isEqualTo("sub_123");
        assertThat(update.planCode()).isEqualTo("PRO");
        assertThat(update.status()).isEqualTo(TenantSubscriptionStatus.ACTIVE);
        assertThat(update.currentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1752592000));
        assertThat(update.occurredAt()).isEqualTo(Instant.ofEpochSecond(1750000300));
    }

    @Test
    void ignoresNonSubscriptionEvents() {
        StripeSubscriptionEventMapper mapper =
                new StripeSubscriptionEventMapper(JsonMapper.builder().build());

        Optional<BillingSubscriptionUpdate> result =
                mapper.map(
                        new VerifiedBillingEvent(
                                BillingProviderType.STRIPE,
                                "evt_123",
                                "invoice.paid",
                                "{}"));

        assertThat(result).isEmpty();
    }
}
