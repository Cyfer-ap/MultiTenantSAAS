package com.chacha.multitenantsaas.billing.entity;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.SubscriptionPlan;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "subscription_plan_provider_mappings",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_plan_provider_price_ref",
                    columnNames = {"provider", "environment", "provider_price_id"}),
            @UniqueConstraint(
                    name = "uk_plan_provider_plan_ref",
                    columnNames = {"provider", "environment", "provider_plan_id"})
        },
        indexes = {
            @Index(
                    name = "idx_plan_provider_mapping_lookup",
                    columnList = "plan_id,provider,environment,status")
        })
public class SubscriptionPlanProviderMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, updatable = false)
    private BillingProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private BillingProviderEnvironment environment;

    @Column(name = "provider_product_id", length = 255, updatable = false)
    private String providerProductId;

    @Column(name = "provider_price_id", length = 255, updatable = false)
    private String providerPriceId;

    @Column(name = "provider_plan_id", length = 255, updatable = false)
    private String providerPlanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlanProviderMappingStatus status =
            SubscriptionPlanProviderMappingStatus.ACTIVE;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SubscriptionPlanProviderMapping() {}

    public SubscriptionPlanProviderMapping(
            SubscriptionPlan plan,
            BillingProviderType provider,
            BillingProviderEnvironment environment,
            String providerProductId,
            String providerPriceId,
            String providerPlanId) {
        this.plan = plan;
        this.provider = provider;
        this.environment = environment;
        this.providerProductId = providerProductId;
        this.providerPriceId = providerPriceId;
        this.providerPlanId = providerPlanId;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = SubscriptionPlanProviderMappingStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void archive(Instant archivedAt) {
        this.status = SubscriptionPlanProviderMappingStatus.ARCHIVED;
        this.archivedAt = archivedAt == null ? Instant.now() : archivedAt;
    }

    public UUID getId() {
        return id;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public BillingProviderType getProvider() {
        return provider;
    }

    public BillingProviderEnvironment getEnvironment() {
        return environment;
    }

    public String getProviderProductId() {
        return providerProductId;
    }

    public String getProviderPriceId() {
        return providerPriceId;
    }

    public String getProviderPlanId() {
        return providerPlanId;
    }

    public SubscriptionPlanProviderMappingStatus getStatus() {
        return status;
    }

    public Instant getArchivedAt() {
        return archivedAt;
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
}
