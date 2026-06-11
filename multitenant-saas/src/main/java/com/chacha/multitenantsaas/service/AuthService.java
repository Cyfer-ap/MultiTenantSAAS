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

import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtContextService jwtContextService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtContextService jwtContextService,
            RefreshTokenService refreshTokenService
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtContextService = jwtContextService;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse login(UUID tenantId, LoginRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        AppUser user = appUserRepository.findByTenantIdAndEmail(tenantId, normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("User account is not active");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new AuthenticationFailedException("Password is not set for this user");
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(tenant, user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

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

        return new TokenRefreshResponse(
                newAccessToken,
                refreshTokenData.refreshToken(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                "Token refreshed successfully"
        );
    }

    public LogoutResponse logout(LogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());

        return new LogoutResponse("Logout successful");
    }
}