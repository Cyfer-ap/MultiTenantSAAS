package com.chacha.multitenantsaas.entity;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_subscriptions",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_tenant_subscription_tenant", columnNames = "tenant_id"),
            @UniqueConstraint(
                    name = "uk_tenant_subscription_provider_ref",
                    columnNames = {"billing_provider", "provider_subscription_id"})
        },
        indexes = {
            @Index(name = "idx_tenant_subscription_plan_status", columnList = "plan_id,status"),
            @Index(
                    name = "idx_tenant_subscription_period_end",
                    columnList = "status,current_period_end")
        })
public class TenantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantSubscriptionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_provider", length = 32)
    private BillingProviderType billingProvider;

    @Column(name = "provider_subscription_id", length = 255)
    private String providerSubscriptionId;

    @Column(name = "provider_event_created_at")
    private Instant providerEventCreatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TenantSubscription() {}

    public TenantSubscription(
            Tenant tenant,
            SubscriptionPlan plan,
            TenantSubscriptionStatus status,
            Instant startedAt,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            Instant trialEndsAt,
            boolean cancelAtPeriodEnd) {
        this.tenant = tenant;
        this.plan = plan;
        this.status = status;
        this.startedAt = startedAt;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.trialEndsAt = trialEndsAt;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
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

    public Tenant getTenant() {
        return tenant;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public TenantSubscriptionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public BillingProviderType getBillingProvider() {
        return billingProvider;
    }

    public String getProviderSubscriptionId() {
        return providerSubscriptionId;
    }

    public Instant getProviderEventCreatedAt() {
        return providerEventCreatedAt;
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

    public void setStatus(TenantSubscriptionStatus status) {
        this.status = status;
    }

    public void setCurrentPeriodStart(Instant currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public void setTrialEndsAt(Instant trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public void setBillingProvider(BillingProviderType billingProvider) {
        this.billingProvider = billingProvider;
    }

    public void setProviderSubscriptionId(String providerSubscriptionId) {
        this.providerSubscriptionId = providerSubscriptionId;
    }

    public void setProviderEventCreatedAt(Instant providerEventCreatedAt) {
        this.providerEventCreatedAt = providerEventCreatedAt;
    }
}
