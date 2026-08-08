package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.SystemAdminCreateRequest;
import com.chacha.multitenantsaas.dto.SystemAdminResponse;
import com.chacha.multitenantsaas.dto.SystemAdminStatusUpdateRequest;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chacha.multitenantsaas.entity.PlatformAuditAction;
import java.util.UUID;
import com.chacha.multitenantsaas.entity.PlatformAuditAction;

@Service
public class SystemAdminManagementService {

    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentSystemAdminService currentSystemAdminService;
    private final SystemAdminGuardService systemAdminGuardService;
    private final LoginAttemptService loginAttemptService;
    private final PlatformAuditLogService platformAuditLogService;

    public SystemAdminManagementService(
            SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder,
            CurrentSystemAdminService currentSystemAdminService,
            SystemAdminGuardService systemAdminGuardService,
            LoginAttemptService loginAttemptService,
            PlatformAuditLogService platformAuditLogService
    ) {
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentSystemAdminService = currentSystemAdminService;
        this.systemAdminGuardService = systemAdminGuardService;
        this.loginAttemptService = loginAttemptService;
        this.platformAuditLogService = platformAuditLogService;
    }

    public SystemAdminResponse createSystemAdmin(
            SystemAdminCreateRequest request,
            Jwt jwt
    ) {
        SystemAdmin actorSystemAdmin =
                currentSystemAdminService.getRequiredActiveSystemAdmin(jwt);

        String normalizedEmail = normalizeEmail(request.email());

        if (systemAdminRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "System admin email already exists: " + normalizedEmail
            );
        }

        SystemAdmin systemAdmin = new SystemAdmin(
                request.fullName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password())
        );

        SystemAdmin savedSystemAdmin =
                systemAdminRepository.save(systemAdmin);

        platformAuditLogService.recordSuccess(
                actorSystemAdmin,
                savedSystemAdmin,
                PlatformAuditAction.SYSTEM_ADMIN_CREATED,
                "System admin created successfully: " + normalizedEmail
        );

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

    public SystemAdminResponse getSystemAdminById(UUID systemAdminId) {
        SystemAdmin systemAdmin = getSystemAdminOrThrow(systemAdminId);

        return mapToResponse(systemAdmin);
    }

    @Transactional
    public SystemAdminResponse updateSystemAdminStatus(
            UUID systemAdminId,
            SystemAdminStatusUpdateRequest request,
            Jwt jwt
    ) {
        SystemAdmin actorSystemAdmin =
                currentSystemAdminService.getRequiredActiveSystemAdmin(jwt);

        SystemAdmin targetSystemAdmin =
                getSystemAdminForUpdateOrThrow(systemAdminId);

        UserStatus oldStatus = targetSystemAdmin.getStatus();

        systemAdminGuardService.ensureCanChangeStatus(
                actorSystemAdmin,
                targetSystemAdmin,
                request.status()
        );

        targetSystemAdmin.setStatus(request.status());

        SystemAdmin updatedSystemAdmin =
                systemAdminRepository.save(targetSystemAdmin);

        platformAuditLogService.recordSuccess(
                actorSystemAdmin,
                updatedSystemAdmin,
                PlatformAuditAction.SYSTEM_ADMIN_STATUS_UPDATED,
                "System admin status updated for "
                        + updatedSystemAdmin.getEmail()
                        + " from " + oldStatus
                        + " to " + updatedSystemAdmin.getStatus()
        );

        return mapToResponse(updatedSystemAdmin);
    }

    @Transactional
    public SystemAdminResponse unlockSystemAdminLogin(
            UUID systemAdminId,
            Jwt jwt
    ) {
        SystemAdmin actorSystemAdmin =
                currentSystemAdminService.getRequiredActiveSystemAdmin(jwt);

        SystemAdmin targetSystemAdmin =
                getSystemAdminForUpdateOrThrow(systemAdminId);

        loginAttemptService.unlockSystemAdmin(targetSystemAdmin);

        platformAuditLogService.recordSuccess(
                actorSystemAdmin,
                targetSystemAdmin,
                PlatformAuditAction.SYSTEM_ADMIN_LOGIN_UNLOCKED,
                "System admin login unlocked successfully: "
                        + targetSystemAdmin.getEmail()
        );

        return mapToResponse(targetSystemAdmin);
    }

    private SystemAdmin getSystemAdminForUpdateOrThrow(
            UUID systemAdminId
    ) {
        return systemAdminRepository.findByIdForUpdate(systemAdminId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System admin not found with id: " + systemAdminId
                ));
    }

    private SystemAdmin getSystemAdminOrThrow(UUID systemAdminId) {
        return systemAdminRepository.findById(systemAdminId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System admin not found with id: " + systemAdminId
                ));
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