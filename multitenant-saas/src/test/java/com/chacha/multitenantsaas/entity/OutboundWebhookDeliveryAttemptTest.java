package com.chacha.multitenantsaas.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboundWebhookDeliveryAttemptTest {

    @Test
    void recordsImmutableSuccessfulOutcome() {
        Instant startedAt = Instant.parse("2026-09-07T12:00:00Z");
        OutboundWebhookDelivery delivery = newDelivery(startedAt);
        UUID leaseToken = UUID.randomUUID();
        OutboundWebhookDeliveryAttempt attempt =
                new OutboundWebhookDeliveryAttempt(delivery, 2, 1, leaseToken, startedAt);

        Instant completedAt = startedAt.plusSeconds(2);
        attempt.markSuccess(completedAt, 204);
        attempt.markFailure(completedAt.plusSeconds(1), 500, "must not overwrite success");

        assertThat(attempt.getReplayNumber()).isEqualTo(2);
        assertThat(attempt.getAttemptNumber()).isEqualTo(1);
        assertThat(attempt.getLeaseToken()).isEqualTo(leaseToken);
        assertThat(attempt.getOutcome()).isEqualTo(OutboundWebhookDeliveryAttemptOutcome.SUCCESS);
        assertThat(attempt.getHttpStatus()).isEqualTo(204);
        assertThat(attempt.getError()).isNull();
        assertThat(attempt.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void recordsFailureAndTruncatesError() {
        Instant startedAt = Instant.parse("2026-09-07T12:00:00Z");
        OutboundWebhookDeliveryAttempt attempt =
                new OutboundWebhookDeliveryAttempt(
                        newDelivery(startedAt), 0, 3, UUID.randomUUID(), startedAt);

        String oversizedError = "x".repeat(1100);
        attempt.markFailure(startedAt.plusSeconds(4), 503, oversizedError);

        assertThat(attempt.getOutcome()).isEqualTo(OutboundWebhookDeliveryAttemptOutcome.FAILURE);
        assertThat(attempt.getHttpStatus()).isEqualTo(503);
        assertThat(attempt.getError()).hasSize(1000);
        assertThat(attempt.getCompletedAt()).isEqualTo(startedAt.plusSeconds(4));
    }

    private OutboundWebhookDelivery newDelivery(Instant now) {
        Tenant tenant = new Tenant("Webhook Tenant", "attempt-tenant");
        AppUser actor =
                new AppUser(
                        tenant,
                        "Webhook Admin",
                        "attempt-admin@example.com",
                        "not-used",
                        UserRole.TENANT_ADMIN);
        OutboundWebhookEndpoint endpoint =
                new OutboundWebhookEndpoint(
                        tenant,
                        "Attempt webhook",
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
