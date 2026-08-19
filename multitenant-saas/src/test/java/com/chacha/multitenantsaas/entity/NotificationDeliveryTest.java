package com.chacha.multitenantsaas.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationDeliveryTest {

    @Test
    void appliesBoundedExponentialBackoffAndStopsAtMaximumAttempts() {
        Instant now = Instant.parse("2026-08-19T10:00:00Z");
        NotificationDelivery delivery =
                new NotificationDelivery(
                        new Tenant(), new Notification(), NotificationDeliveryChannel.EMAIL, now);

        UUID firstLease = delivery.claim(now);
        delivery.markFailedAttempt(
                firstLease,
                now,
                "temporary failure",
                3,
                Duration.ofSeconds(10),
                Duration.ofSeconds(15));
        assertThat(delivery.getNextAttemptAt()).isEqualTo(now.plusSeconds(10));

        UUID secondLease = delivery.claim(now.plusSeconds(10));
        delivery.markFailedAttempt(
                secondLease,
                now.plusSeconds(10),
                "temporary failure",
                3,
                Duration.ofSeconds(10),
                Duration.ofSeconds(15));
        assertThat(delivery.getNextAttemptAt()).isEqualTo(now.plusSeconds(25));

        UUID finalLease = delivery.claim(now.plusSeconds(25));
        delivery.markFailedAttempt(
                finalLease,
                now.plusSeconds(25),
                "permanent failure",
                3,
                Duration.ofSeconds(10),
                Duration.ofSeconds(15));

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(3);
        assertThat(delivery.getNextAttemptAt()).isNull();
        assertThat(delivery.getLastError()).isEqualTo("permanent failure");
    }
}
