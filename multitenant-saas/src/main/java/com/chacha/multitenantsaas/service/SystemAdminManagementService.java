package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.SystemAdminCreateRequest;
import com.chacha.multitenantsaas.dto.SystemAdminResponse;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SystemAdminManagementService {

    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public SystemAdminManagementService(
            SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SystemAdminResponse createSystemAdmin(SystemAdminCreateRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (systemAdminRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("System admin email already exists: " + normalizedEmail);
        }

        SystemAdmin systemAdmin = new SystemAdmin(
                request.fullName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password())
        );

        SystemAdmin savedSystemAdmin = systemAdminRepository.save(systemAdmin);

        return mapToResponse(savedSystemAdmin);
    }

    public PageResponse<SystemAdminResponse> getSystemAdmins(
            UserStatus status,
            String search,
            Pageable pageable
    ) {
        Page<SystemAdmin> systemAdmins = systemAdminRepository.findSystemAdmins(
                status,
                normalizeSearch(search),
                pageable
        );

        return new PageResponse<>(
                systemAdmins.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList(),
                systemAdmins.getNumber(),
                systemAdmins.getSize(),
                systemAdmins.getTotalElements(),
                systemAdmins.getTotalPages(),
                systemAdmins.isFirst(),
                systemAdmins.isLast()
        );
    }

    private SystemAdminResponse mapToResponse(SystemAdmin systemAdmin) {
        return new SystemAdminResponse(
                systemAdmin.getId(),
                systemAdmin.getFullName(),
                systemAdmin.getEmail(),
                systemAdmin.getStatus(),
                systemAdmin.getFailedLoginAttempts(),
                systemAdmin.getLockedUntil(),
                systemAdmin.getCreatedAt(),
                systemAdmin.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeSearch(String search) {
        if (search == null || search.trim().isBlank()) {
            return null;
        }

        return search.trim();
    }
}
