package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.LoginRequest;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.chacha.multitenantsaas.dto.CurrentUserResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import com.chacha.multitenantsaas.security.AuthenticatedUserContext;
import com.chacha.multitenantsaas.security.JwtContextService;
import com.chacha.multitenantsaas.dto.RefreshTokenRequest;
import com.chacha.multitenantsaas.dto.TokenRefreshResponse;
import com.chacha.multitenantsaas.dto.LogoutRequest;
import com.chacha.multitenantsaas.dto.LogoutResponse;
import com.chacha.multitenantsaas.dto.ChangePasswordRequest;
import com.chacha.multitenantsaas.dto.ChangePasswordResponse;
import com.chacha.multitenantsaas.entity.AuditAction;

import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtContextService jwtContextService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtContextService jwtContextService,
            RefreshTokenService refreshTokenService,
            AuditLogService auditLogService,
            LoginAttemptService loginAttemptService
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtContextService = jwtContextService;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
        this.loginAttemptService = loginAttemptService;
    }

    public LoginResponse login(UUID tenantId, LoginRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            auditLogService.record(
                    tenant,
                    null,
                    AuditAction.LOGIN_FAILED,
                    false,
                    "Login failed because tenant is not active"
            );

            throw new AuthenticationFailedException("Tenant is not active");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        AppUser user = appUserRepository.findByTenantIdAndEmail(tenantId, normalizedEmail)
                .orElseThrow(() -> {
                    auditLogService.record(
                            tenant,
                            null,
                            AuditAction.LOGIN_FAILED,
                            false,
                            "Login failed due to invalid email"
                    );

                    return new AuthenticationFailedException("Invalid email or password");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            auditLogService.record(
                    tenant,
                    user,
                    AuditAction.LOGIN_FAILED,
                    false,
                    "Login failed because user account is not active"
            );

            throw new AuthenticationFailedException("User account is not active");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            auditLogService.record(
                    tenant,
                    user,
                    AuditAction.LOGIN_FAILED,
                    false,
                    "Login failed because password is not set"
            );

            throw new AuthenticationFailedException("Password is not set for this user");
        }

        loginAttemptService.ensureNotLocked(user);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            loginAttemptService.recordFailedAttempt(user);

            auditLogService.record(
                    tenant,
                    user,
                    AuditAction.LOGIN_FAILED,
                    false,
                    "Login failed due to invalid password"
            );

            throw new AuthenticationFailedException("Invalid email or password");
        }

        loginAttemptService.recordSuccessfulLogin(user);

        String accessToken = jwtService.generateAccessToken(tenant, user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        auditLogService.record(
                tenant,
                user,
                AuditAction.LOGIN_SUCCESS,
                true,
                "User logged in successfully"
        );

        return new LoginResponse(
                tenant.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                "Login successful"
        );
    }

    public CurrentUserResponse getCurrentUser(Jwt jwt) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        Tenant tenant = tenantRepository.findById(currentUser.tenantId())
                .orElseThrow(() -> new AuthenticationFailedException("Tenant not found"));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }

        AppUser user = appUserRepository.findByTenantIdAndId(
                currentUser.tenantId(),
                currentUser.userId()
        ).orElseThrow(() -> new AuthenticationFailedException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("User account is not active");
        }

        return new CurrentUserResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }

    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenService.RefreshTokenData refreshTokenData =
                refreshTokenService.rotateRefreshToken(request.refreshToken());

        String newAccessToken = jwtService.generateAccessToken(
                refreshTokenData.tenant(),
                refreshTokenData.user()
        );

        auditLogService.record(
                refreshTokenData.tenant(),
                refreshTokenData.user(),
                AuditAction.TOKEN_REFRESH,
                true,
                "Access token refreshed successfully"
        );

        return new TokenRefreshResponse(
                newAccessToken,
                refreshTokenData.refreshToken(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                "Token refreshed successfully"
        );
    }

    public LogoutResponse logout(LogoutRequest request) {
        RefreshTokenService.RefreshTokenData refreshTokenData =
                refreshTokenService.revokeRefreshTokenAndReturnData(request.refreshToken());

        auditLogService.record(
                refreshTokenData.tenant(),
                refreshTokenData.user(),
                AuditAction.LOGOUT,
                true,
                "User logged out successfully"
        );

        return new LogoutResponse("Logout successful");
    }

    public LogoutResponse logoutAllDevices(Jwt jwt) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        Tenant tenant = tenantRepository.findById(currentUser.tenantId())
                .orElseThrow(() -> new AuthenticationFailedException("Tenant not found"));

        AppUser user = appUserRepository.findByTenantIdAndId(
                currentUser.tenantId(),
                currentUser.userId()
        ).orElseThrow(() -> new AuthenticationFailedException("User not found"));

        refreshTokenService.revokeAllActiveTokensForUser(currentUser.userId());

        auditLogService.record(
                tenant,
                user,
                AuditAction.LOGOUT_ALL,
                true,
                "User logged out from all devices"
        );

        return new LogoutResponse("Logged out from all devices successfully");
    }

    public ChangePasswordResponse changePassword(Jwt jwt, ChangePasswordRequest request) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        AppUser user = appUserRepository.findByTenantIdAndId(
                currentUser.tenantId(),
                currentUser.userId()
        ).orElseThrow(() -> new AuthenticationFailedException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("User account is not active");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new AuthenticationFailedException("Password is not set for this user");
        }

        boolean currentPasswordMatches = passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        );

        if (!currentPasswordMatches) {
            throw new AuthenticationFailedException("Current password is incorrect");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        String newPasswordHash = passwordEncoder.encode(request.newPassword());

        user.setPasswordHash(newPasswordHash);
        appUserRepository.save(user);

        refreshTokenService.revokeAllActiveTokensForUser(user.getId());

        Tenant tenant = tenantRepository.findById(currentUser.tenantId())
                .orElseThrow(() -> new AuthenticationFailedException("Tenant not found"));

        auditLogService.record(
                tenant,
                user,
                AuditAction.PASSWORD_CHANGED,
                true,
                "User changed password successfully"
        );

        return new ChangePasswordResponse(
                "Password changed successfully. Please login again."
        );
    }
}