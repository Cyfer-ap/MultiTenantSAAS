package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
                @Index(name = "idx_audit_actor_user", columnList = "actor_user_id"),
                @Index(name = "idx_audit_actor_system_admin", columnList = "actor_system_admin_id"),
                @Index(name = "idx_audit_target_user", columnList = "target_user_id"),
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

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 30)
    private AuditActorType actorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_system_admin_id")
    private SystemAdmin actorSystemAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private AppUser targetUser;

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

    public AuditLog(
            Tenant tenant,
            AppUser actorUser,
            AppUser targetUser,
            AuditAction action,
            boolean success,
            String message
    ) {
        this.tenant = tenant;
        this.actorType = actorUser == null ? AuditActorType.SYSTEM : AuditActorType.TENANT_USER;
        this.actorUser = actorUser;
        this.targetUser = targetUser;
        this.action = action;
        this.success = success;
        this.message = message;
    }

    public AuditLog(
            Tenant tenant,
            SystemAdmin actorSystemAdmin,
            AppUser targetUser,
            AuditAction action,
            boolean success,
            String message
    ) {
        this.tenant = tenant;
        this.actorType = actorSystemAdmin == null ? AuditActorType.SYSTEM : AuditActorType.SYSTEM_ADMIN;
        this.actorSystemAdmin = actorSystemAdmin;
        this.targetUser = targetUser;
        this.action = action;
        this.success = success;
        this.message = message;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();

        if (this.actorType == null) {
            if (this.actorSystemAdmin != null) {
                this.actorType = AuditActorType.SYSTEM_ADMIN;
            } else if (this.actorUser != null) {
                this.actorType = AuditActorType.TENANT_USER;
            } else {
                this.actorType = AuditActorType.SYSTEM;
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public AuditActorType getActorType() {
        return actorType;
    }

    public AppUser getActorUser() {
        return actorUser;
    }

    public SystemAdmin getActorSystemAdmin() {
        return actorSystemAdmin;
    }

    public AppUser getTargetUser() {
        return targetUser;
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

    public void setActorType(AuditActorType actorType) {
        this.actorType = actorType;
    }

    public void setActorUser(AppUser actorUser) {
        this.actorUser = actorUser;
    }

    public void setActorSystemAdmin(SystemAdmin actorSystemAdmin) {
        this.actorSystemAdmin = actorSystemAdmin;
    }

    public void setTargetUser(AppUser targetUser) {
        this.targetUser = targetUser;
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