package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentSystemAdminService {

    private final SystemAdminRepository systemAdminRepository;

    public CurrentSystemAdminService(SystemAdminRepository systemAdminRepository) {
        this.systemAdminRepository = systemAdminRepository;
    }

    public SystemAdmin getRequiredActiveSystemAdmin(Jwt jwt) {
        if (jwt == null) {
            throw new AuthenticationFailedException("Authentication token is required");
        }

        String role = jwt.getClaimAsString("role");
        String accountType = jwt.getClaimAsString("accountType");

        if (!"SYSTEM_ADMIN".equals(role) || !"SYSTEM_ADMIN".equals(accountType)) {
            throw new AuthenticationFailedException("Invalid system admin token");
        }

        UUID systemAdminId = parseUuid(jwt.getSubject());

        if (systemAdminId == null) {
            throw new AuthenticationFailedException("Invalid system admin token subject");
        }

        SystemAdmin systemAdmin = systemAdminRepository.findById(systemAdminId)
                .orElseThrow(() -> new AuthenticationFailedException("System admin not found"));

        if (systemAdmin.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("System admin account is not active");
        }

        return systemAdmin;
    }

    public boolean isSystemAdminToken(Jwt jwt) {
        if (jwt == null) {
            return false;
        }

        String role = jwt.getClaimAsString("role");
        String accountType = jwt.getClaimAsString("accountType");

        return "SYSTEM_ADMIN".equals(role) && "SYSTEM_ADMIN".equals(accountType);
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}