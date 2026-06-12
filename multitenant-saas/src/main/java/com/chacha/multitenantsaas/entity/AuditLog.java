package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
                @Index(name = "idx_audit_user", columnList = "user_id"),
                @Index(name = "idx_audit_action", columnList = "action"),
                @Index(name = "idx_audit_created_at", columnList = "created_at")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AuditAction action;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 500)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog() {
    }

    public AuditLog(Tenant tenant, AppUser user, AuditAction action, boolean success, String message) {
        this.tenant = tenant;
        this.user = user;
        this.action = action;
        this.success = success;
        this.message = message;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
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

    public AuditAction getAction() {
        return action;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}