package com.chacha.multitenantsaas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbound_webhook_deliveries",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_outbound_webhook_delivery_event_endpoint",
                        columnNames = {"event_id", "endpoint_id"}))
public class OutboundWebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private OutboundWebhookEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private OutboundWebhookEndpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboundWebhookDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "replay_count", nullable = false)
    private int replayCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "lease_token")
    private UUID leaseToken;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

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

    public OutboundWebhookDelivery() {}

    public OutboundWebhookDelivery(
            Tenant tenant,
            OutboundWebhookEvent event,
            OutboundWebhookEndpoint endpoint,
            Instant now) {
        this.tenant = tenant;
        this.event = event;
        this.endpoint = endpoint;
        this.status = OutboundWebhookDeliveryStatus.PENDING;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID claim(Instant now) {
        status = OutboundWebhookDeliveryStatus.PROCESSING;
        attemptCount++;
        processingStartedAt = now;
        nextAttemptAt = null;
        leaseToken = UUID.randomUUID();
        updatedAt = now;
        return leaseToken;
    }

    public boolean markSent(UUID expectedLeaseToken, Instant now, int httpStatus) {
        if (!ownsLease(expectedLeaseToken)) return false;
        status = OutboundWebhookDeliveryStatus.SENT;
        sentAt = now;
        lastHttpStatus = httpStatus;
        lastError = null;
        clearLease();
        updatedAt = now;
        return true;
    }

    public boolean markFailedAttempt(
            UUID expectedLeaseToken,
            Instant now,
            Integer httpStatus,
            String error,
            int maxAttempts,
            Duration baseDelay,
            Duration maxDelay) {
        if (!ownsLease(expectedLeaseToken)) return false;
        lastHttpStatus = httpStatus;
        lastError = truncate(error);
        clearLease();
        if (attemptCount >= maxAttempts) {
            status = OutboundWebhookDeliveryStatus.FAILED;
            nextAttemptAt = null;
        } else {
            status = OutboundWebhookDeliveryStatus.RETRY;
            nextAttemptAt = now.plus(backoff(baseDelay, maxDelay));
        }
        updatedAt = now;
        return true;
    }

    public void failExpiredFinalLease(Instant now) {
        status = OutboundWebhookDeliveryStatus.FAILED;
        lastError = "Webhook delivery lease expired after final attempt";
        clearLease();
        nextAttemptAt = null;
        updatedAt = now;
    }

    public void replay(Instant now) {
        if (status != OutboundWebhookDeliveryStatus.SENT
                && status != OutboundWebhookDeliveryStatus.FAILED) {
            throw new IllegalArgumentException("Only sent or failed webhook deliveries can be replayed");
        }
        status = OutboundWebhookDeliveryStatus.PENDING;
        attemptCount = 0;
        replayCount++;
        nextAttemptAt = now;
        processingStartedAt = null;
        leaseToken = null;
        lastHttpStatus = null;
        lastError = null;
        sentAt = null;
        updatedAt = now;
    }

    private boolean ownsLease(UUID expectedLeaseToken) {
        return status == OutboundWebhookDeliveryStatus.PROCESSING
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
        String safe = error == null || error.isBlank() ? "Webhook delivery failed" : error.trim();
        return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public OutboundWebhookEvent getEvent() {
        return event;
    }

    public OutboundWebhookEndpoint getEndpoint() {
        return endpoint;
    }

    public OutboundWebhookDeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getReplayCount() {
        return replayCount;
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

    public Integer getLastHttpStatus() {
        return lastHttpStatus;
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
