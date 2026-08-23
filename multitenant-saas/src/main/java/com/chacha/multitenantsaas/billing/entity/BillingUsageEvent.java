package com.chacha.multitenantsaas.billing.entity;

import com.chacha.multitenantsaas.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "billing_usage_events",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_billing_usage_event_idempotency",
                    columnNames = {"tenant_id", "idempotency_key"})
        },
        indexes = {
            @Index(
                    name = "idx_billing_usage_event_period",
                    columnList = "tenant_id,metric_code,occurred_at")
        })
public class BillingUsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "metric_code", nullable = false, length = 64, updatable = false)
    private String metricCode;

    @Column(nullable = false, updatable = false)
    private long quantity;

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    private String idempotencyKey;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected BillingUsageEvent() {}

    public BillingUsageEvent(
            Tenant tenant,
            String metricCode,
            long quantity,
            String idempotencyKey,
            Instant occurredAt) {
        this.tenant = tenant;
        this.metricCode = metricCode;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    protected void onCreate() {
        this.recordedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public long getQuantity() {
        return quantity;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
