package com.chacha.multitenantsaas.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "platform_audit_logs",
        indexes = {
                @Index(
                        name = "idx_platform_audit_actor",
                        columnList = "actor_system_admin_id"
                ),
                @Index(
                        name = "idx_platform_audit_target",
                        columnList = "target_system_admin_id"
                ),
                @Index(
                        name = "idx_platform_audit_action",
                        columnList = "action"
                ),
                @Index(
                        name = "idx_platform_audit_created_at",
                        columnList = "created_at"
                )
        }
)
public class PlatformAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_system_admin_id")
    private SystemAdmin actorSystemAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_system_admin_id")
    private SystemAdmin targetSystemAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private PlatformAuditAction action;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 500)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public PlatformAuditLog() {
    }

    public PlatformAuditLog(
            SystemAdmin actorSystemAdmin,
            SystemAdmin targetSystemAdmin,
            PlatformAuditAction action,
            boolean success,
            String message
    ) {
        this.actorSystemAdmin = actorSystemAdmin;
        this.targetSystemAdmin = targetSystemAdmin;
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

    public SystemAdmin getActorSystemAdmin() {
        return actorSystemAdmin;
    }

    public SystemAdmin getTargetSystemAdmin() {
        return targetSystemAdmin;
    }

    public PlatformAuditAction getAction() {
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

    public void setActorSystemAdmin(SystemAdmin actorSystemAdmin) {
        this.actorSystemAdmin = actorSystemAdmin;
    }

    public void setTargetSystemAdmin(SystemAdmin targetSystemAdmin) {
        this.targetSystemAdmin = targetSystemAdmin;
    }

    public void setAction(PlatformAuditAction action) {
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