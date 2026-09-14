package com.chacha.multitenantsaas.savedviews.entity;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "saved_views",
        indexes =
                @Index(
                        name = "idx_saved_views_scope",
                        columnList = "tenant_id,user_id,target,context_id,name"))
public class SavedView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "target", nullable = false, length = 32)
    private SavedViewTarget target;

    @Column(name = "context_id")
    private UUID contextId;

    @Column(name = "definition_json", nullable = false, length = 4000)
    private String definitionJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SavedView() {}

    public SavedView(
            Tenant tenant,
            AppUser user,
            String name,
            SavedViewTarget target,
            UUID contextId,
            String definitionJson) {
        this.tenant = tenant;
        this.user = user;
        this.name = name;
        this.target = target;
        this.contextId = contextId;
        this.definitionJson = definitionJson;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(String nextName, String nextDefinitionJson) {
        name = nextName;
        definitionJson = nextDefinitionJson;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AppUser getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public SavedViewTarget getTarget() {
        return target;
    }

    public UUID getContextId() {
        return contextId;
    }

    public String getDefinitionJson() {
        return definitionJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
