package com.chacha.multitenantsaas.billing.entity;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_events")
public class BillingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingProviderType provider;

    @Column(nullable = false, unique = true)
    private String providerEventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    protected BillingEvent() {}
}
