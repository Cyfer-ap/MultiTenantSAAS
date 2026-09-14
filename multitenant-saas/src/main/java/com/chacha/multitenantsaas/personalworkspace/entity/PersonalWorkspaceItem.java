package com.chacha.multitenantsaas.personalworkspace.entity;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.personalworkspace.model.PersonalResourceType;
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
        name = "personal_workspace_items",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_personal_workspace_item_resource",
                        columnNames = {"tenant_id", "user_id", "resource_type", "resource_id"}),
        indexes = {
            @Index(
                    name = "idx_personal_workspace_favorites",
                    columnList = "tenant_id,user_id,favorite_at"),
            @Index(
                    name = "idx_personal_workspace_recent",
                    columnList = "tenant_id,user_id,last_viewed_at")
        })
public class PersonalWorkspaceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private PersonalResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "favorite_at")
    private Instant favoriteAt;

    @Column(name = "last_viewed_at")
    private Instant lastViewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PersonalWorkspaceItem() {}

    public PersonalWorkspaceItem(
            Tenant tenant, AppUser user, PersonalResourceType resourceType, UUID resourceId) {
        this.tenant = tenant;
        this.user = user;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
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

    public void markFavorite(Instant at) {
        favoriteAt = at;
    }

    public void clearFavorite() {
        favoriteAt = null;
    }

    public void markViewed(Instant at) {
        lastViewedAt = at;
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

    public PersonalResourceType getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Instant getFavoriteAt() {
        return favoriteAt;
    }

    public Instant getLastViewedAt() {
        return lastViewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
