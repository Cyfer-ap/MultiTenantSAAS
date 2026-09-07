package com.chacha.multitenantsaas.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboundWebhookDeliveryTest {

    @Test
    void ownsLeaseAndAppliesCappedRetryLifecycle() {
        Instant now = Instant.parse("2026-09-07T12:00:00Z");
        OutboundWebhookDelivery delivery = newDelivery(now);

        assertThat(delivery.getStatus()).isEqualTo(OutboundWebhookDeliveryStatus.PENDING);
        assertThat(delivery.getAttemptCount()).isZero();
        assertThat(delivery.getNextAttemptAt()).isEqualTo(now);

        UUID firstLease = delivery.claim(now);
        assertThat(delivery.getStatus()).isEqualTo(OutboundWebhookDeliveryStatus.PROCESSING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(
                        delivery.markFailedAttempt(
                                UUID.randomUUID(),
                                now,
                                500,
                                "wrong lease",
                                3,
                                Duration.ofSeconds(10),
                                Duration.ofMinutes(1)))
                .isFalse();

        assertThat(
                        delivery.markFailedAttempt(
                                firstLease,
                                now,
                                500,
                                "receiver failed",
                                3,
                                Duration.ofSeconds(10),
                                Duration.ofMinutes(1)))
                .isTrue();
        assertThat(delivery.getStatus()).isEqualTo(OutboundWebhookDeliveryStatus.RETRY);
        assertThat(delivery.getNextAttemptAt()).isEqualTo(now.plusSeconds(10));

        Instant secondAttempt = now.plusSeconds(10);
        UUID secondLease = delivery.claim(secondAttempt);
        assertThat(
                        delivery.markFailedAttempt(
                                secondLease,
                                secondAttempt,
                                503,
                                "unavailable",
                                3,
                                Duration.ofSeconds(10),
                                Duration.ofMinutes(1)))
                .isTrue();
        assertThat(delivery.getNextAttemptAt()).isEqualTo(now.plusSeconds(30));

        Instant thirdAttempt = now.plusSeconds(30);
        UUID thirdLease = delivery.claim(thirdAttempt);
        assertThat(
                        delivery.markFailedAttempt(
                                thirdLease,
                                thirdAttempt,
                                503,
                                "still unavailable",
                                3,
                                Duration.ofSeconds(10),
                                Duration.ofMinutes(1)))
                .isTrue();
        assertThat(delivery.getStatus()).isEqualTo(OutboundWebhookDeliveryStatus.FAILED);
        assertThat(delivery.getNextAttemptAt()).isNull();
        assertThat(delivery.getLastHttpStatus()).isEqualTo(503);
    }

    @Test
    void marksOnlyCurrentLeaseAsSent() {
        Instant now = Instant.parse("2026-09-07T12:00:00Z");
        OutboundWebhookDelivery delivery = newDelivery(now);
        UUID lease = delivery.claim(now);

        assertThat(delivery.markSent(UUID.randomUUID(), now, 200)).isFalse();
        assertThat(delivery.markSent(lease, now.plusSeconds(1), 204)).isTrue();
        assertThat(delivery.getStatus()).isEqualTo(OutboundWebhookDeliveryStatus.SENT);
        assertThat(delivery.getSentAt()).isEqualTo(now.plusSeconds(1));
        assertThat(delivery.getLastHttpStatus()).isEqualTo(204);
    }

    private OutboundWebhookDelivery newDelivery(Instant now) {
        Tenant tenant = new Tenant("Webhook Tenant", "webhook-tenant");
        AppUser actor =
                new AppUser(
                        tenant,
                        "Webhook Admin",
                        "webhook-admin@example.com",
                        "not-used",
                        UserRole.TENANT_ADMIN);
        OutboundWebhookEndpoint endpoint =
                new OutboundWebhookEndpoint(
                        tenant,
                        "Primary webhook",
                        "https://8.8.8.8/hooks",
                        true,
                        Set.of(OutboundWebhookEventType.PROJECT_CREATED),
                        "encrypted-secret",
                        "****secret",
                        actor,
                        now);
        OutboundWebhookEvent event =
                new OutboundWebhookEvent(
                        UUID.randomUUID(),
                        tenant,
                        OutboundWebhookEventType.PROJECT_CREATED,
                        "{\"id\":\"event\"}",
                        now);
        return new OutboundWebhookDelivery(tenant, event, endpoint, now);
    }
}
