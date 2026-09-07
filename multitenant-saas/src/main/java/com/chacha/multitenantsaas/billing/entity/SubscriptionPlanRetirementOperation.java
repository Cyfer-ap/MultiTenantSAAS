package com.chacha.multitenantsaas.billing.entity;

import com.chacha.multitenantsaas.entity.SubscriptionPlan;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plan_retirement_operations")
public class SubscriptionPlanRetirementOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, unique = true)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlanRetirementStatus status = SubscriptionPlanRetirementStatus.PENDING;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    private Instant startedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected SubscriptionPlanRetirementOperation() {}

    public SubscriptionPlanRetirementOperation(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public void requestRetry(Instant now) {
        status = SubscriptionPlanRetirementStatus.PENDING;
        lastError = null;
        requestedAt = now;
        completedAt = null;
        updatedAt = now;
    }

    public void markProcessing(Instant now) {
        status = SubscriptionPlanRetirementStatus.PROCESSING;
        attemptCount++;
        startedAt = now;
        lastError = null;
        updatedAt = now;
    }

    public void markCompleted(Instant now) {
        status = SubscriptionPlanRetirementStatus.COMPLETED;
        completedAt = now;
        lastError = null;
        updatedAt = now;
    }

    public void markFailed(String error, Instant now) {
        status = SubscriptionPlanRetirementStatus.FAILED;
        lastError = error;
        completedAt = null;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public SubscriptionPlanRetirementStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
