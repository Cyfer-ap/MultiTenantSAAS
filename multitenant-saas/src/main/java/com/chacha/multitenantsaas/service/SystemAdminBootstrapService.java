package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SystemAdminBootstrapService {

    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;

    private final boolean bootstrapEnabled;
    private final String fullName;
    private final String email;
    private final String password;

    public SystemAdminBootstrapService(
            SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.system-admin.bootstrap.enabled:false}") boolean bootstrapEnabled,
            @Value("${app.system-admin.bootstrap.full-name:}") String fullName,
            @Value("${app.system-admin.bootstrap.email:}") String email,
            @Value("${app.system-admin.bootstrap.password:}") String password
    ) {
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEnabled = bootstrapEnabled;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    @PostConstruct
    public void bootstrapSystemAdmin() {
        if (!bootstrapEnabled) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalStateException("System admin bootstrap email is required");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalStateException("System admin bootstrap password is required");
        }

        if (systemAdminRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        SystemAdmin systemAdmin = new SystemAdmin(
                fullName == null || fullName.isBlank() ? "System Admin" : fullName.trim(),
                normalizedEmail,
                passwordEncoder.encode(password)
        );

        systemAdminRepository.save(systemAdmin);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}