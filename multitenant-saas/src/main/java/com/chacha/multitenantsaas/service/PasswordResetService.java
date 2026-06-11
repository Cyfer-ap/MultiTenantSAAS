package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.ForgotPasswordRequest;
import com.chacha.multitenantsaas.dto.ForgotPasswordResponse;
import com.chacha.multitenantsaas.dto.ResetPasswordRequest;
import com.chacha.multitenantsaas.dto.ResetPasswordResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.PasswordResetToken;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.PasswordResetTokenRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final long expirationMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            @Value("${app.password-reset.expiration-minutes}") long expirationMinutes
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(UUID tenantId, ForgotPasswordRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        AppUser user = appUserRepository.findByTenantIdAndEmail(tenantId, normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("User account is not active");
        }

        revokeExistingUnusedResetTokens(user.getId());

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken(
                user,
                tokenHash,
                Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES)
        );

        passwordResetTokenRepository.save(resetToken);

        return new ForgotPasswordResponse(
                "Password reset token generated successfully. In production, this should be sent by email.",
                rawToken
        );
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        String tokenHash = hashToken(request.resetToken());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid reset token"));

        if (!resetToken.isActive()) {
            throw new AuthenticationFailedException("Reset token is expired or already used");
        }

        AppUser user = resetToken.getUser();
        Tenant tenant = user.getTenant();

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("User account is not active");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        refreshTokenService.revokeAllActiveTokensForUser(user.getId());

        return new ResetPasswordResponse(
                "Password reset successfully. Please login again."
        );
    }

    private void revokeExistingUnusedResetTokens(UUID userId) {
        List<PasswordResetToken> existingTokens =
                passwordResetTokenRepository.findByUserIdAndUsedFalse(userId);

        existingTokens.forEach(token -> {
            token.setUsed(true);
            token.setUsedAt(Instant.now());
        });

        passwordResetTokenRepository.saveAll(existingTokens);
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
            throw new IllegalStateException("Could not hash reset token", exception);
        }
    }
}