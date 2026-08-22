package com.chacha.multitenantsaas.billing.entity;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "billing_customers")
public class BillingCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingProviderType provider;

    @Column(nullable = false, unique = true)
    private String providerCustomerId;

    protected BillingCustomer() {}

    public BillingCustomer(UUID tenantId, BillingProviderType provider, String providerCustomerId) {
        this.tenantId = tenantId;
        this.provider = provider;
        this.providerCustomerId = providerCustomerId;
    }
}
