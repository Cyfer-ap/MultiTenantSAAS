package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component("tenantSecurity")
public class TenantSecurityService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;

    public TenantSecurityService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
    }

    public boolean isSameTenant(UUID tenantId) {
        return getActiveUserForTenant(tenantId) != null;
    }

    public boolean isSameTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .map(Tenant::getId)
                .map(this::isSameTenant)
                .orElse(false);
    }

    public boolean isTenantAdmin(UUID tenantId) {
        return hasRoleForTenant(tenantId, "TENANT_ADMIN");
    }

    public boolean isTenantAdminOrManager(UUID tenantId) {
        return hasRoleForTenant(tenantId, "TENANT_ADMIN", "TENANT_MANAGER");
    }

    public boolean isCurrentTenantAdmin() {
        UUID tenantId = getCurrentTenantId();

        if (tenantId == null) {
            return false;
        }

        return isTenantAdmin(tenantId);
    }

    public boolean isCurrentTenantAdminOrManager() {
        UUID tenantId = getCurrentTenantId();

        if (tenantId == null) {
            return false;
        }

        return isTenantAdminOrManager(tenantId);
    }

    private boolean hasRoleForTenant(UUID tenantId, String... allowedRoles) {
        AppUser user = getActiveUserForTenant(tenantId);

        if (user == null) {
            return false;
        }

        return Arrays.asList(allowedRoles).contains(user.getRole().name());
    }

    private AppUser getActiveUserForTenant(UUID tenantId) {
        Jwt jwt = getJwt();

        if (jwt == null) {
            return null;
        }

        UUID tokenTenantId = parseUuid(jwt.getClaimAsString("tenantId"));
        UUID tokenUserId = parseUuid(jwt.getSubject());

        if (tokenTenantId == null || tokenUserId == null) {
            return null;
        }

        if (!tenantId.equals(tokenTenantId)) {
            return null;
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);

        if (tenant == null || tenant.getStatus() != TenantStatus.ACTIVE) {
            return null;
        }

        AppUser user = appUserRepository.findByTenantIdAndId(
                tenantId,
                tokenUserId
        ).orElse(null);

        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return null;
        }

        return user;
    }

    private UUID getCurrentTenantId() {
        Jwt jwt = getJwt();

        if (jwt == null) {
            return null;
        }

        return parseUuid(jwt.getClaimAsString("tenantId"));
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        return jwt;
    }
}