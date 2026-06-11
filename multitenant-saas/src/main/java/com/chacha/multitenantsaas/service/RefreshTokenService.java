package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.RefreshToken;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.refresh-token.expiration-days}") long expirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationDays = expirationDays;
    }

    @Transactional
    public String createRefreshToken(AppUser user) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                Instant.now().plus(expirationDays, ChronoUnit.DAYS)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RefreshTokenData rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token"));

        if (!existingToken.isActive()) {
            throw new AuthenticationFailedException("Refresh token is expired or revoked");
        }

        AppUser user = existingToken.getUser();
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

        String newRefreshToken = createRefreshToken(user);

        return new RefreshTokenData(tenant, user, newRefreshToken);
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[64];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte hashedByte : hashedBytes) {
                hexString.append(String.format("%02x", hashedByte));
            }

            return hexString.toString();

        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash refresh token", exception);
        }
    }

    public record RefreshTokenData(
            Tenant tenant,
            AppUser user,
            String refreshToken
    ) {
    }
}