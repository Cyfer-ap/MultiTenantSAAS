package com.chacha.multitenantsaas.billing.entity;

import com.chacha.multitenantsaas.entity.SubscriptionPlan;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "subscription_plan_usage_limits",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_subscription_plan_usage_limit_metric",
                    columnNames = {"plan_id", "metric_code"})
        },
        indexes = {@Index(name = "idx_subscription_plan_usage_limit_plan", columnList = "plan_id")})
public class SubscriptionPlanUsageLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "metric_code", nullable = false, length = 64)
    private String metricCode;

    @Column(name = "period_limit", nullable = false)
    private long periodLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SubscriptionPlanUsageLimit() {}

    public SubscriptionPlanUsageLimit(SubscriptionPlan plan, String metricCode, long periodLimit) {
        this.plan = plan;
        this.metricCode = metricCode;
        this.periodLimit = periodLimit;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public String getMetricCode() {
        return metricCode;
    }

    public long getPeriodLimit() {
        return periodLimit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }

    public void setPeriodLimit(long periodLimit) {
        this.periodLimit = periodLimit;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
