package com.chacha.multitenantsaas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbound_webhook_delivery_attempts",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_outbound_webhook_attempt_lease",
                        columnNames = {"delivery_id", "lease_token"}),
        indexes = {
            @Index(
                    name = "idx_outbound_webhook_attempt_delivery_started",
                    columnList = "delivery_id, started_at"),
            @Index(
                    name = "idx_outbound_webhook_attempt_tenant_started",
                    columnList = "tenant_id, started_at")
        })
public class OutboundWebhookDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private OutboundWebhookDelivery delivery;

    @Column(name = "replay_number", nullable = false)
    private int replayNumber;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "lease_token", nullable = false)
    private UUID leaseToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboundWebhookDeliveryAttemptOutcome outcome;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "error", length = 1000)
    private String error;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected OutboundWebhookDeliveryAttempt() {}

    public OutboundWebhookDeliveryAttempt(
            OutboundWebhookDelivery delivery,
            int replayNumber,
            int attemptNumber,
            UUID leaseToken,
            Instant startedAt) {
        this.tenant = delivery.getTenant();
        this.delivery = delivery;
        this.replayNumber = replayNumber;
        this.attemptNumber = attemptNumber;
        this.leaseToken = leaseToken;
        this.outcome = OutboundWebhookDeliveryAttemptOutcome.PROCESSING;
        this.startedAt = startedAt;
    }

    public void markSuccess(Instant completedAt, int httpStatus) {
        if (outcome != OutboundWebhookDeliveryAttemptOutcome.PROCESSING) {
            return;
        }
        outcome = OutboundWebhookDeliveryAttemptOutcome.SUCCESS;
        this.httpStatus = httpStatus;
        error = null;
        this.completedAt = completedAt;
    }

    public void markFailure(Instant completedAt, Integer httpStatus, String error) {
        if (outcome != OutboundWebhookDeliveryAttemptOutcome.PROCESSING) {
            return;
        }
        outcome = OutboundWebhookDeliveryAttemptOutcome.FAILURE;
        this.httpStatus = httpStatus;
        this.error = truncate(error);
        this.completedAt = completedAt;
    }

    private String truncate(String value) {
        String safe = value == null || value.isBlank() ? "Webhook delivery failed" : value.trim();
        return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public OutboundWebhookDelivery getDelivery() {
        return delivery;
    }

    public int getReplayNumber() {
        return replayNumber;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public UUID getLeaseToken() {
        return leaseToken;
    }

    public OutboundWebhookDeliveryAttemptOutcome getOutcome() {
        return outcome;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getError() {
        return error;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
