package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_notification_delivery_channel",
                        columnNames = {"tenant_id", "notification_id", "channel"}))
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationDeliveryChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "lease_token")
    private UUID leaseToken;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public NotificationDelivery() {}

    public NotificationDelivery(
            Tenant tenant,
            Notification notification,
            NotificationDeliveryChannel channel,
            Instant now) {
        this.tenant = tenant;
        this.notification = notification;
        this.channel = channel;
        this.status = NotificationDeliveryStatus.PENDING;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID claim(Instant now) {
        status = NotificationDeliveryStatus.PROCESSING;
        attemptCount++;
        processingStartedAt = now;
        nextAttemptAt = null;
        leaseToken = UUID.randomUUID();
        updatedAt = now;
        return leaseToken;
    }

    public boolean markSent(UUID expectedLeaseToken, Instant now) {
        if (!ownsLease(expectedLeaseToken)) return false;
        status = NotificationDeliveryStatus.SENT;
        sentAt = now;
        lastError = null;
        clearLease();
        updatedAt = now;
        return true;
    }

    public boolean markFailedAttempt(
            UUID expectedLeaseToken,
            Instant now,
            String error,
            int maxAttempts,
            Duration baseDelay,
            Duration maxDelay) {
        if (!ownsLease(expectedLeaseToken)) return false;
        lastError = truncate(error);
        clearLease();
        if (attemptCount >= maxAttempts) {
            status = NotificationDeliveryStatus.FAILED;
            nextAttemptAt = null;
        } else {
            status = NotificationDeliveryStatus.RETRY;
            nextAttemptAt = now.plus(backoff(baseDelay, maxDelay));
        }
        updatedAt = now;
        return true;
    }

    public void failExpiredFinalLease(Instant now) {
        status = NotificationDeliveryStatus.FAILED;
        lastError = "Delivery lease expired after final attempt";
        clearLease();
        nextAttemptAt = null;
        updatedAt = now;
    }

    private boolean ownsLease(UUID expectedLeaseToken) {
        return status == NotificationDeliveryStatus.PROCESSING
                && leaseToken != null
                && leaseToken.equals(expectedLeaseToken);
    }

    private void clearLease() {
        processingStartedAt = null;
        leaseToken = null;
    }

    private Duration backoff(Duration baseDelay, Duration maxDelay) {
        long multiplier = 1L << Math.min(Math.max(attemptCount - 1, 0), 20);
        Duration delay;
        try {
            delay = baseDelay.multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            delay = maxDelay;
        }
        return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
    }

    private String truncate(String error) {
        String safe = error == null || error.isBlank() ? "Delivery failed" : error.trim();
        return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Notification getNotification() {
        return notification;
    }

    public NotificationDeliveryChannel getChannel() {
        return channel;
    }

    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public UUID getLeaseToken() {
        return leaseToken;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
