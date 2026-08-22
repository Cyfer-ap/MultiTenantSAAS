package com.chacha.multitenantsaas.billing.razorpay;

import static org.assertj.core.api.Assertions.assertThat;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.billing.webhook.BillingSubscriptionUpdate;
import com.chacha.multitenantsaas.billing.webhook.VerifiedBillingEvent;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RazorpaySubscriptionEventMapperTest {

    @Test
    void mapsRazorpaySubscriptionNotesAndLifecycle() {
        UUID tenantId = UUID.randomUUID();
        String payload =
                """
                {
                  "created_at": 1750000300,
                  "payload": {"subscription": {"entity": {
                    "id": "sub_razorpay_123",
                    "status": "active",
                    "start_at": 1750000000,
                    "current_start": 1750000000,
                    "current_end": 1752592000,
                    "notes": {"tenant_id": "%s", "plan_code": "PRO"}
                  }}}
                }
                """
                        .formatted(tenantId);
        RazorpaySubscriptionEventMapper mapper =
                new RazorpaySubscriptionEventMapper(JsonMapper.builder().build());

        BillingSubscriptionUpdate update =
                mapper.map(
                                new VerifiedBillingEvent(
                                        BillingProviderType.RAZORPAY,
                                        "event_123",
                                        "subscription.activated",
                                        payload))
                        .orElseThrow();

        assertThat(update.tenantId()).isEqualTo(tenantId);
        assertThat(update.providerSubscriptionId()).isEqualTo("sub_razorpay_123");
        assertThat(update.status()).isEqualTo(TenantSubscriptionStatus.ACTIVE);
        assertThat(update.currentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1752592000));
    }

    @Test
    void mapsCompletedEventToExpired() {
        UUID tenantId = UUID.randomUUID();
        String payload =
                """
                {
                  "created_at": 1752592000,
                  "payload": {"subscription": {"entity": {
                    "id": "sub_razorpay_123",
                    "status": "completed",
                    "start_at": 1750000000,
                    "current_start": 1750000000,
                    "current_end": 1752592000,
                    "notes": {"tenant_id": "%s", "plan_code": "PRO"}
                  }}}
                }
                """
                        .formatted(tenantId);
        RazorpaySubscriptionEventMapper mapper =
                new RazorpaySubscriptionEventMapper(JsonMapper.builder().build());

        BillingSubscriptionUpdate update =
                mapper.map(
                                new VerifiedBillingEvent(
                                        BillingProviderType.RAZORPAY,
                                        "event_456",
                                        "subscription.completed",
                                        payload))
                        .orElseThrow();

        assertThat(update.status()).isEqualTo(TenantSubscriptionStatus.EXPIRED);
    }
}
