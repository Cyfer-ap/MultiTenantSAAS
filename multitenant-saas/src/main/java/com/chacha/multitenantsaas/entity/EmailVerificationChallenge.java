package com.chacha.multitenantsaas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "email_verification_challenges",
        indexes = {
            @Index(name = "idx_email_verification_email", columnList = "email"),
            @Index(name = "idx_email_verification_expires_at", columnList = "expires_at")
        })
public class EmailVerificationChallenge {

    @Id private UUID id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "login_consumed_at")
    private Instant loginConsumedAt;

    public EmailVerificationChallenge() {}

    public EmailVerificationChallenge(String email, String codeHash, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.codeHash = codeHash;
        this.failedAttempts = 0;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getLoginConsumedAt() {
        return loginConsumedAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public void setLoginConsumedAt(Instant loginConsumedAt) {
        this.loginConsumedAt = loginConsumedAt;
    }

    public void recordFailedAttempt() {
        this.failedAttempts++;
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public void markLoginConsumed(Instant now) {
        this.loginConsumedAt = now;
    }

    public boolean isLoginConsumed() {
        return loginConsumedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
