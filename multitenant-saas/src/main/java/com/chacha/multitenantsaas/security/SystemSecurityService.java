package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("systemSecurity")
public class SystemSecurityService {

    private final SystemAdminRepository systemAdminRepository;

    public SystemSecurityService(SystemAdminRepository systemAdminRepository) {
        this.systemAdminRepository = systemAdminRepository;
    }

    public boolean isSystemAdmin() {
        Jwt jwt = getJwt();

        if (jwt == null) {
            return false;
        }

        String role = jwt.getClaimAsString("role");
        String accountType = jwt.getClaimAsString("accountType");
        UUID systemAdminId = parseUuid(jwt.getSubject());

        if (!"SYSTEM_ADMIN".equals(role)) {
            return false;
        }

        if (!"SYSTEM_ADMIN".equals(accountType)) {
            return false;
        }

        if (systemAdminId == null) {
            return false;
        }

        SystemAdmin systemAdmin = systemAdminRepository.findById(systemAdminId)
                .orElse(null);

        return systemAdmin != null
                && systemAdmin.getStatus() == UserStatus.ACTIVE;
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