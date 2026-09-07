package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OidcSessionService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;

    public OidcSessionService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuditLogService auditLogService,
            LoginAttemptService loginAttemptService) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public LoginResponse issue(UUID tenantId, UUID userId, boolean persistentSession) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(this::failed);
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw failed();
        }

        AppUser user =
                appUserRepository
                        .findByTenantIdAndIdForUpdate(tenantId, userId)
                        .orElseThrow(this::failed);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw failed();
        }

        loginAttemptService.recordSuccessfulLogin(user);
        String accessToken = jwtService.generateAccessToken(tenant, user);
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.createIssuedRefreshToken(user, persistentSession);

        auditLogService.record(
                tenant,
                user,
                AuditAction.LOGIN_SUCCESS,
                true,
                "User logged in successfully with verified OIDC identity");

        return new LoginResponse(
                tenant.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                refreshToken.refreshToken(),
                refreshToken.csrfToken(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                refreshToken.persistentSession(),
                "Login successful");
    }

    private AuthenticationFailedException failed() {
        return new AuthenticationFailedException("OIDC authentication failed");
    }
}
