package com.chacha.multitenantsaas.entity;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_subscription_history",
        indexes = {
            @Index(
                    name = "idx_subscription_history_tenant_recorded",
                    columnList = "tenant_id,recorded_at"),
            @Index(
                    name = "idx_subscription_history_subscription_recorded",
                    columnList = "subscription_id,recorded_at"),
            @Index(
                    name = "idx_subscription_history_provider_ref",
                    columnList = "billing_provider,provider_subscription_id")
        })
public class TenantSubscriptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_name_snapshot", nullable = false, length = 200)
    private String tenantNameSnapshot;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "plan_code_snapshot", nullable = false, length = 60)
    private String planCodeSnapshot;

    @Column(name = "plan_name_snapshot", nullable = false, length = 150)
    private String planNameSnapshot;

    @Column(name = "plan_description_snapshot", length = 500)
    private String planDescriptionSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval_snapshot", nullable = false, length = 20)
    private BillingInterval billingIntervalSnapshot;

    @Column(name = "price_snapshot", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "currency_snapshot", nullable = false, length = 3)
    private String currencySnapshot;

    @Column(name = "max_users_snapshot")
    private Integer maxUsersSnapshot;

    @Column(name = "max_projects_snapshot")
    private Integer maxProjectsSnapshot;

    @Column(name = "max_storage_mb_snapshot")
    private Long maxStorageMbSnapshot;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private TenantSubscriptionHistoryEventType eventType;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected TenantSubscriptionHistory() {}

    public TenantSubscriptionHistory(
            TenantSubscription subscription, TenantSubscriptionHistoryEventType eventType) {
        this.subscriptionId = subscription.getId();
        this.tenantId = subscription.getTenant().getId();
        this.tenantNameSnapshot = subscription.getTenant().getName();
        this.planId = subscription.getPlan().getId();
        this.planCodeSnapshot = subscription.getPlanCodeSnapshot();
        this.planNameSnapshot = subscription.getPlanNameSnapshot();
        this.planDescriptionSnapshot = subscription.getPlanDescriptionSnapshot();
        this.billingIntervalSnapshot = subscription.getBillingIntervalSnapshot();
        this.priceSnapshot = subscription.getPriceSnapshot();
        this.currencySnapshot = subscription.getCurrencySnapshot();
        this.maxUsersSnapshot = subscription.getMaxUsersSnapshot();
        this.maxProjectsSnapshot = subscription.getMaxProjectsSnapshot();
        this.maxStorageMbSnapshot = subscription.getMaxStorageMbSnapshot();
        this.status = subscription.getStatus();
        this.startedAt = subscription.getStartedAt();
        this.currentPeriodStart = subscription.getCurrentPeriodStart();
        this.currentPeriodEnd = subscription.getCurrentPeriodEnd();
        this.trialEndsAt = subscription.getTrialEndsAt();
        this.cancelAtPeriodEnd = subscription.isCancelAtPeriodEnd();
        this.cancelledAt = subscription.getCancelledAt();
        this.billingProvider = subscription.getBillingProvider();
        this.providerSubscriptionId = subscription.getProviderSubscriptionId();
        this.providerEventCreatedAt = subscription.getProviderEventCreatedAt();
        this.eventType = eventType;
    }

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTenantNameSnapshot() {
        return tenantNameSnapshot;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getPlanCodeSnapshot() {
        return planCodeSnapshot;
    }

    public String getPlanNameSnapshot() {
        return planNameSnapshot;
    }

    public String getPlanDescriptionSnapshot() {
        return planDescriptionSnapshot;
    }

    public BillingInterval getBillingIntervalSnapshot() {
        return billingIntervalSnapshot;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }

    public String getCurrencySnapshot() {
        return currencySnapshot;
    }

    public Integer getMaxUsersSnapshot() {
        return maxUsersSnapshot;
    }

    public Integer getMaxProjectsSnapshot() {
        return maxProjectsSnapshot;
    }

    public Long getMaxStorageMbSnapshot() {
        return maxStorageMbSnapshot;
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

    public TenantSubscriptionHistoryEventType getEventType() {
        return eventType;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
