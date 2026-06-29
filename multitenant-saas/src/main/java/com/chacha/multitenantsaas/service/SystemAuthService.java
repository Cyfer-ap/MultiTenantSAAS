package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.SystemAdminLoginRequest;
import com.chacha.multitenantsaas.dto.SystemAdminLoginResponse;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}