package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "outbound_webhook_endpoints",
        indexes = {
            @Index(
                    name = "idx_outbound_webhook_endpoint_tenant_created",
                    columnList = "tenant_id, created_at"),
            @Index(
                    name = "idx_outbound_webhook_endpoint_tenant_archived",
                    columnList = "tenant_id, archived_at")
        })
public class OutboundWebhookEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "secret_ciphertext", nullable = false, length = 4096)
    private String secretCiphertext;

    @Column(name = "secret_hint", nullable = false, length = 32)
    private String secretHint;

    @Column(name = "secret_version", nullable = false)
    private int secretVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "outbound_webhook_endpoint_events",
            joinColumns = @JoinColumn(name = "endpoint_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private Set<OutboundWebhookEventType> eventTypes = EnumSet.noneOf(OutboundWebhookEventType.class);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdByUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_user_id", nullable = false)
    private AppUser updatedByUser;

    @Column(name = "secret_rotated_at", nullable = false)
    private Instant secretRotatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public OutboundWebhookEndpoint() {}

    public OutboundWebhookEndpoint(
            Tenant tenant,
            String name,
            String url,
            boolean enabled,
            Set<OutboundWebhookEventType> eventTypes,
            String secretCiphertext,
            String secretHint,
            AppUser actor,
            Instant now) {
        this.tenant = tenant;
        this.name = name;
        this.url = url;
        this.enabled = enabled;
        this.eventTypes = copyEvents(eventTypes);
        this.secretCiphertext = secretCiphertext;
        this.secretHint = secretHint;
        this.secretVersion = 1;
        this.createdByUser = actor;
        this.updatedByUser = actor;
        this.secretRotatedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String name,
            String url,
            boolean enabled,
            Set<OutboundWebhookEventType> eventTypes,
            AppUser actor,
            Instant now) {
        this.name = name;
        this.url = url;
        this.enabled = enabled;
        this.eventTypes = copyEvents(eventTypes);
        this.updatedByUser = actor;
        this.updatedAt = now;
    }

    public void rotateSecret(
            String newSecretCiphertext, String newSecretHint, AppUser actor, Instant now) {
        this.secretCiphertext = newSecretCiphertext;
        this.secretHint = newSecretHint;
        this.secretVersion++;
        this.secretRotatedAt = now;
        this.updatedByUser = actor;
        this.updatedAt = now;
    }

    public void archive(AppUser actor, Instant now) {
        if (archivedAt == null) {
            archivedAt = now;
            enabled = false;
            updatedByUser = actor;
            updatedAt = now;
        }
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    private static Set<OutboundWebhookEventType> copyEvents(
            Set<OutboundWebhookEventType> eventTypes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return EnumSet.noneOf(OutboundWebhookEventType.class);
        }
        return EnumSet.copyOf(eventTypes);
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSecretCiphertext() {
        return secretCiphertext;
    }

    public String getSecretHint() {
        return secretHint;
    }

    public int getSecretVersion() {
        return secretVersion;
    }

    public Set<OutboundWebhookEventType> getEventTypes() {
        return Set.copyOf(eventTypes);
    }

    public AppUser getCreatedByUser() {
        return createdByUser;
    }

    public AppUser getUpdatedByUser() {
        return updatedByUser;
    }

    public Instant getSecretRotatedAt() {
        return secretRotatedAt;
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
}
