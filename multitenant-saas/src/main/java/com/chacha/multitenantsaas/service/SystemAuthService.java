package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.ChangePasswordRequest;
import com.chacha.multitenantsaas.dto.SystemAdminCurrentResponse;
import com.chacha.multitenantsaas.dto.SystemAdminLoginRequest;
import com.chacha.multitenantsaas.dto.SystemAdminLoginResponse;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SystemAuthService {

    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public SystemAuthService(
            SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public SystemAdminLoginResponse login(SystemAdminLoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        SystemAdmin systemAdmin = systemAdminRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (systemAdmin.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("System admin account is not active");
        }

        if (!passwordEncoder.matches(request.password(), systemAdmin.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        String accessToken = jwtService.generateSystemAdminAccessToken(systemAdmin);

        return new SystemAdminLoginResponse(
                systemAdmin.getId(),
                systemAdmin.getFullName(),
                systemAdmin.getEmail(),
                "SYSTEM_ADMIN",
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                "System admin login successful"
        );
    }

    public SystemAdminCurrentResponse getCurrentSystemAdmin(Jwt jwt) {
        SystemAdmin systemAdmin = getRequiredActiveSystemAdmin(jwt);

        return mapToCurrentResponse(systemAdmin);
    }

    public SystemAdminCurrentResponse changePassword(
            Jwt jwt,
            ChangePasswordRequest request
    ) {
        SystemAdmin systemAdmin = getRequiredActiveSystemAdmin(jwt);

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if (!passwordEncoder.matches(request.currentPassword(), systemAdmin.getPasswordHash())) {
            throw new AuthenticationFailedException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), systemAdmin.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        systemAdmin.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        SystemAdmin updatedSystemAdmin = systemAdminRepository.save(systemAdmin);

        return mapToCurrentResponse(updatedSystemAdmin);
    }

    private SystemAdmin getRequiredActiveSystemAdmin(Jwt jwt) {
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

    private SystemAdminCurrentResponse mapToCurrentResponse(SystemAdmin systemAdmin) {
        return new SystemAdminCurrentResponse(
                systemAdmin.getId(),
                systemAdmin.getFullName(),
                systemAdmin.getEmail(),
                "SYSTEM_ADMIN",
                systemAdmin.getStatus()
        );
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}