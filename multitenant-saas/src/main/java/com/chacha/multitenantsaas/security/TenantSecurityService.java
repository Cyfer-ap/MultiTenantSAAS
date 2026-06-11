package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.Tenant;
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

    public TenantSecurityService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public boolean isSameTenant(UUID tenantId) {
        Jwt jwt = getJwt();

        if (jwt == null) {
            return false;
        }

        String tokenTenantId = jwt.getClaimAsString("tenantId");

        return tenantId.toString().equals(tokenTenantId);
    }

    public boolean isSameTenantBySlug(String slug) {
        Jwt jwt = getJwt();

        if (jwt == null) {
            return false;
        }

        String tokenTenantId = jwt.getClaimAsString("tenantId");

        return tenantRepository.findBySlug(slug)
                .map(Tenant::getId)
                .map(id -> id.toString().equals(tokenTenantId))
                .orElse(false);
    }

    public boolean isTenantAdmin(UUID tenantId) {
        return hasRoleForTenant(tenantId, "TENANT_ADMIN");
    }

    public boolean isTenantAdminOrManager(UUID tenantId) {
        return hasRoleForTenant(tenantId, "TENANT_ADMIN", "TENANT_MANAGER");
    }

    private boolean hasRoleForTenant(UUID tenantId, String... allowedRoles) {
        Jwt jwt = getJwt();

        if (jwt == null) {
            return false;
        }

        String tokenTenantId = jwt.getClaimAsString("tenantId");
        String role = jwt.getClaimAsString("role");

        boolean sameTenant = tenantId.toString().equals(tokenTenantId);
        boolean allowedRole = Arrays.asList(allowedRoles).contains(role);

        return sameTenant && allowedRole;
    }

    private Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        return jwt;
    }
}
