package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.RefreshToken;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository appUserRepository;
    private final long sessionExpirationHours;
    private final long persistentExpirationDays;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AppUserRepository appUserRepository,
            @Value("${app.refresh-token.session-expiration-hours:12}") long sessionExpirationHours,
            @Value("${app.refresh-token.persistent-expiration-days:30}")
                    long persistentExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.appUserRepository = appUserRepository;
        this.sessionExpirationHours = sessionExpirationHours;
        this.persistentExpirationDays = persistentExpirationDays;

        if (sessionExpirationHours <= 0L || persistentExpirationDays <= 0L) {
            throw new IllegalStateException("Refresh-token expiration settings must be positive");
        }
    }

    // Compatibility constructor used by existing focused unit tests.
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AppUserRepository appUserRepository,
            long expirationDays) {
        this(
                refreshTokenRepository,
                appUserRepository,
                Math.multiplyExact(expirationDays, 24L),
                expirationDays);
    }

    @Transactional
    public String createRefreshToken(AppUser user) {
        return createIssuedRefreshToken(user, false).refreshToken();
    }

    @Transactional
    public IssuedRefreshToken createIssuedRefreshToken(AppUser user, boolean persistentSession) {
        Instant expiresAt =
                persistentSession
                        ? Instant.now().plus(persistentExpirationDays, ChronoUnit.DAYS)
                        : Instant.now().plus(sessionExpirationHours, ChronoUnit.HOURS);

        return issueRefreshToken(user, persistentSession, expiresAt);
    }

    private IssuedRefreshToken issueRefreshToken(
            AppUser user, boolean persistentSession, Instant expiresAt) {
        String rawRefreshToken = generateSecureToken();
        String rawCsrfToken = generateSecureToken();

        RefreshToken refreshToken =
                new RefreshToken(
                        user,
                        hashToken(rawRefreshToken),
                        hashToken(rawCsrfToken),
                        persistentSession,
                        expiresAt);

        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(rawRefreshToken, rawCsrfToken, persistentSession, expiresAt);
    }

    @Transactional
    public RefreshTokenData rotateRefreshToken(String rawRefreshToken) {
        return rotateRefreshToken(rawRefreshToken, null, false);
    }

    @Transactional
    public RefreshTokenData rotateRefreshToken(String rawRefreshToken, String rawCsrfToken) {
        return rotateRefreshToken(rawRefreshToken, rawCsrfToken, true);
    }

    private RefreshTokenData rotateRefreshToken(
            String rawRefreshToken, String rawCsrfToken, boolean requireCsrf) {
        String tokenHash = hashToken(rawRefreshToken);

        UUID userId =
                refreshTokenRepository
                        .findUserIdByTokenHash(tokenHash)
                        .orElseThrow(
                                () -> new AuthenticationFailedException("Invalid refresh token"));

        AppUser user =
                appUserRepository
                        .findByIdForUpdate(userId)
                        .orElseThrow(
                                () ->
                                        new AuthenticationFailedException(
                                                "Refresh-token user not found"));

        RefreshToken existingToken =
                refreshTokenRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(
                                () -> new AuthenticationFailedException("Invalid refresh token"));

        if (!existingToken.getUser().getId().equals(user.getId())) {
            throw new AuthenticationFailedException("Invalid refresh token");
        }

        if (!existingToken.isActive()) {
            throw new AuthenticationFailedException("Refresh token is expired or revoked");
        }

        if (requireCsrf) {
            validateCsrf(existingToken, rawCsrfToken);
        }

        Tenant tenant = user.getTenant();

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("User account is not active");
        }

        existingToken.setRevoked(true);
        existingToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existingToken);

        IssuedRefreshToken issuedRefreshToken =
                issueRefreshToken(
                        user, existingToken.isPersistentSession(), existingToken.getExpiresAt());

        return new RefreshTokenData(
                tenant,
                user,
                issuedRefreshToken.refreshToken(),
                issuedRefreshToken.csrfToken(),
                issuedRefreshToken.persistentSession(),
                issuedRefreshToken.expiresAt());
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[64];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash refresh token", exception);
        }
    }

    private void validateCsrf(RefreshToken refreshToken, String rawCsrfToken) {
        if (rawCsrfToken == null
                || rawCsrfToken.isBlank()
                || refreshToken.getCsrfTokenHash() == null
                || refreshToken.getCsrfTokenHash().isBlank()) {
            throw new AuthenticationFailedException("Invalid session credentials");
        }

        String submittedHash = hashToken(rawCsrfToken.trim());

        if (!MessageDigest.isEqual(
                refreshToken.getCsrfTokenHash().getBytes(StandardCharsets.UTF_8),
                submittedHash.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthenticationFailedException("Invalid session credentials");
        }
    }

    public record IssuedRefreshToken(
            String refreshToken, String csrfToken, boolean persistentSession, Instant expiresAt) {}

    public record RefreshTokenData(
            Tenant tenant,
            AppUser user,
            String refreshToken,
            String csrfToken,
            boolean persistentSession,
            Instant expiresAt) {}

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        revokeRefreshTokenAndReturnData(rawRefreshToken);
    }

    @Transactional
    public void revokeAllActiveTokensForUser(UUID userId) {
        appUserRepository
                .findByIdForUpdate(userId)
                .orElseThrow(
                        () -> new AuthenticationFailedException("Refresh-token user not found"));

        List<RefreshToken> activeTokens =
                refreshTokenRepository.findByUserIdAndRevokedFalse(userId);

        activeTokens.forEach(
                refreshToken -> {
                    if (!refreshToken.isExpired()) {
                        refreshToken.setRevoked(true);
                        refreshToken.setRevokedAt(Instant.now());
                    }
                });

        refreshTokenRepository.saveAll(activeTokens);
    }

    @Transactional
    public void revokeAllActiveTokensForTenant(UUID tenantId) {
        List<RefreshToken> activeTokens =
                refreshTokenRepository.findByUser_Tenant_IdAndRevokedFalse(tenantId);

        activeTokens.forEach(
                refreshToken -> {
                    if (!refreshToken.isExpired()) {
                        refreshToken.setRevoked(true);
                        refreshToken.setRevokedAt(Instant.now());
                    }
                });

        refreshTokenRepository.saveAll(activeTokens);
    }

    @Transactional
    public RefreshTokenData revokeRefreshTokenAndReturnData(String rawRefreshToken) {
        return revokeRefreshTokenAndReturnData(rawRefreshToken, null, false);
    }

    @Transactional
    public RefreshTokenData revokeRefreshTokenAndReturnData(
            String rawRefreshToken, String rawCsrfToken) {
        return revokeRefreshTokenAndReturnData(rawRefreshToken, rawCsrfToken, true);
    }

    private RefreshTokenData revokeRefreshTokenAndReturnData(
            String rawRefreshToken, String rawCsrfToken, boolean requireCsrf) {
        String tokenHash = hashToken(rawRefreshToken);

        UUID userId =
                refreshTokenRepository
                        .findUserIdByTokenHash(tokenHash)
                        .orElseThrow(
                                () -> new AuthenticationFailedException("Invalid refresh token"));

        AppUser user =
                appUserRepository
                        .findByIdForUpdate(userId)
                        .orElseThrow(
                                () ->
                                        new AuthenticationFailedException(
                                                "Refresh-token user not found"));

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(
                                () -> new AuthenticationFailedException("Invalid refresh token"));

        if (!refreshToken.getUser().getId().equals(user.getId())) {
            throw new AuthenticationFailedException("Invalid refresh token");
        }

        if (requireCsrf) {
            validateCsrf(refreshToken, rawCsrfToken);
        }

        Tenant tenant = user.getTenant();

        if (!refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(refreshToken);
        }

        return new RefreshTokenData(
                tenant,
                user,
                rawRefreshToken,
                rawCsrfToken,
                refreshToken.isPersistentSession(),
                refreshToken.getExpiresAt());
    }
}
