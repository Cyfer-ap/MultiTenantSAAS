package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.ChangePasswordRequest;
import com.chacha.multitenantsaas.dto.ChangePasswordResponse;
import com.chacha.multitenantsaas.dto.CurrentUserResponse;
import com.chacha.multitenantsaas.dto.LoginRequest;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.dto.LogoutRequest;
import com.chacha.multitenantsaas.dto.LogoutResponse;
import com.chacha.multitenantsaas.dto.RefreshTokenRequest;
import com.chacha.multitenantsaas.dto.TokenRefreshResponse;
import com.chacha.multitenantsaas.dto.VerifiedTenantLoginRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.EmailVerificationChallenge;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthenticatedUserContext;
import com.chacha.multitenantsaas.security.JwtContextService;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService;

    public AuthService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtContextService jwtContextService,
            RefreshTokenService refreshTokenService,
            AuditLogService auditLogService,
            LoginAttemptService loginAttemptService,
            EmailWorkspaceDiscoveryService emailWorkspaceDiscoveryService) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtContextService = jwtContextService;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
        this.loginAttemptService = loginAttemptService;
        this.emailWorkspaceDiscoveryService = emailWorkspaceDiscoveryService;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public LoginResponse loginVerified(UUID tenantId, VerifiedTenantLoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        EmailVerificationChallenge loginGrant =
                emailWorkspaceDiscoveryService.requireActiveLoginGrant(
                        request.workspaceGrantId(), normalizedEmail);

        LoginResponse response =
                loginInternal(
                        tenantId,
                        new LoginRequest(request.email(), request.password()),
                        Boolean.TRUE.equals(request.keepSignedIn()));

        emailWorkspaceDiscoveryService.consumeLoginGrant(loginGrant);
        return response;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public LoginResponse login(UUID tenantId, LoginRequest request) {
        return loginInternal(tenantId, request, false);
    }

    private LoginResponse loginInternal(
            UUID tenantId, LoginRequest request, boolean persistentSession) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tenant not found with id: " + tenantId));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            auditLogService.record(
                    tenant,
                    null,
                    AuditAction.LOGIN_FAILED,
                    false,
                    "Login failed because tenant is not active");

            throw new AuthenticationFailedException("Tenant is not active");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        AppUser user =
                appUserRepository
                        .findByTenantIdAndEmailForUpdate(tenantId, normalizedEmail)
                        .orElseThrow(
                                () -> {
                                    auditLogService.record(
                                            tenant,
                                            null,
                                            AuditAction.LOGIN_FAILED,
                                            false,
                                            "Login failed due to invalid email");

                                    return new AuthenticationFailedException(
                                            "Invalid email or password");
                                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            auditLogService.record(
                    tenant,
                    user,
                    AuditAction.LOGIN_FAILED,
                    false,
                    "Login failed because user account is not active");

            throw new AuthenticationFailedException("User account is not active");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            auditLogService.record(
                    tenant,
                    user,
                    AuditAction.LOGIN_FAILED,
                    false,
                    "Login failed because password is not set");

            throw new AuthenticationFailedException("Password is not set for this user");
        }

        loginAttemptService.ensureNotLocked(user);

        boolean passwordMatches =
                passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!passwordMatches) {
            loginAttemptService.recordFailedAttempt(user);

            auditLogService.record(
                    tenant,
                    user,
                    AuditAction.LOGIN_FAILED,
                    false,
                    "Login failed due to invalid password");

            throw new AuthenticationFailedException("Invalid email or password");
        }

        loginAttemptService.recordSuccessfulLogin(user);

        String accessToken = jwtService.generateAccessToken(tenant, user);
        RefreshTokenService.IssuedRefreshToken issuedRefreshToken =
                refreshTokenService.createIssuedRefreshToken(user, persistentSession);

        auditLogService.record(
                tenant, user, AuditAction.LOGIN_SUCCESS, true, "User logged in successfully");

        return new LoginResponse(
                tenant.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                issuedRefreshToken.refreshToken(),
                issuedRefreshToken.csrfToken(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                issuedRefreshToken.persistentSession(),
                "Login successful");
    }

    public CurrentUserResponse getCurrentUser(Jwt jwt) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        Tenant tenant =
                tenantRepository
                        .findById(currentUser.tenantId())
                        .orElseThrow(() -> new AuthenticationFailedException("Tenant not found"));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }

        AppUser user =
                appUserRepository
                        .findByTenantIdAndId(currentUser.tenantId(), currentUser.userId())
                        .orElseThrow(() -> new AuthenticationFailedException("User not found"));

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
                user.getStatus());
    }

    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        return createRefreshResponse(
                refreshTokenService.rotateRefreshToken(request.refreshToken()));
    }

    public TokenRefreshResponse refreshToken(String rawRefreshToken, String rawCsrfToken) {
        return createRefreshResponse(
                refreshTokenService.rotateRefreshToken(rawRefreshToken, rawCsrfToken));
    }

    private TokenRefreshResponse createRefreshResponse(
            RefreshTokenService.RefreshTokenData refreshTokenData) {
        String newAccessToken =
                jwtService.generateAccessToken(refreshTokenData.tenant(), refreshTokenData.user());

        auditLogService.record(
                refreshTokenData.tenant(),
                refreshTokenData.user(),
                AuditAction.TOKEN_REFRESH,
                true,
                "Access token refreshed successfully");

        return new TokenRefreshResponse(
                newAccessToken,
                refreshTokenData.refreshToken(),
                refreshTokenData.csrfToken(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                refreshTokenData.persistentSession(),
                "Token refreshed successfully");
    }

    public LogoutResponse logout(LogoutRequest request) {
        return logout(refreshTokenService.revokeRefreshTokenAndReturnData(request.refreshToken()));
    }

    public LogoutResponse logout(String rawRefreshToken, String rawCsrfToken) {
        return logout(
                refreshTokenService.revokeRefreshTokenAndReturnData(rawRefreshToken, rawCsrfToken));
    }

    private LogoutResponse logout(RefreshTokenService.RefreshTokenData refreshTokenData) {
        auditLogService.record(
                refreshTokenData.tenant(),
                refreshTokenData.user(),
                AuditAction.LOGOUT,
                true,
                "User logged out successfully");

        return new LogoutResponse("Logout successful");
    }

    @Transactional
    public LogoutResponse logoutAllDevices(Jwt jwt) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        Tenant tenant =
                tenantRepository
                        .findById(currentUser.tenantId())
                        .orElseThrow(() -> new AuthenticationFailedException("Tenant not found"));

        AppUser user =
                appUserRepository
                        .findByTenantIdAndIdForUpdate(currentUser.tenantId(), currentUser.userId())
                        .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        user.incrementSessionVersion();
        appUserRepository.save(user);

        refreshTokenService.revokeAllActiveTokensForUser(currentUser.userId());

        auditLogService.record(
                tenant, user, AuditAction.LOGOUT_ALL, true, "User logged out from all devices");

        return new LogoutResponse("Logged out from all devices successfully");
    }

    @Transactional
    public ChangePasswordResponse changePassword(Jwt jwt, ChangePasswordRequest request) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        AppUser user =
                appUserRepository
                        .findByTenantIdAndIdForUpdate(currentUser.tenantId(), currentUser.userId())
                        .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("User account is not active");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new AuthenticationFailedException("Password is not set for this user");
        }

        boolean currentPasswordMatches =
                passwordEncoder.matches(request.currentPassword(), user.getPasswordHash());

        if (!currentPasswordMatches) {
            throw new AuthenticationFailedException("Current password is incorrect");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "New password must be different from current password");
        }

        String newPasswordHash = passwordEncoder.encode(request.newPassword());

        user.setPasswordHash(newPasswordHash);
        user.incrementSessionVersion();
        appUserRepository.save(user);

        refreshTokenService.revokeAllActiveTokensForUser(user.getId());

        Tenant tenant =
                tenantRepository
                        .findById(currentUser.tenantId())
                        .orElseThrow(() -> new AuthenticationFailedException("Tenant not found"));

        auditLogService.record(
                tenant,
                user,
                AuditAction.PASSWORD_CHANGED,
                true,
                "User changed password successfully");

        return new ChangePasswordResponse("Password changed successfully. Please login again.");
    }
}
