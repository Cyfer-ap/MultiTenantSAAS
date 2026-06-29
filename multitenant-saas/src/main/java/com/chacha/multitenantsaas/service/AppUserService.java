package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AppUserCreateRequest;
import com.chacha.multitenantsaas.dto.AppUserResponse;
import com.chacha.multitenantsaas.dto.AppUserRoleUpdateRequest;
import com.chacha.multitenantsaas.dto.AppUserStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.AppUserUpdateRequest;
import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CurrentActorService currentActorService;

    public AppUserService(
            AppUserRepository appUserRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            CurrentActorService currentActorService
    ) {
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.currentActorService = currentActorService;
    }

    public AppUserResponse createUser(
            UUID tenantId,
            AppUserCreateRequest request,
            Jwt jwt
    ) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        AppUser actorUser = currentActorService.getRequiredActiveActor(tenantId, jwt);

        String normalizedEmail = request.email().trim().toLowerCase();

        if (appUserRepository.existsByTenantIdAndEmail(tenantId, normalizedEmail)) {
            throw new DuplicateResourceException("User email already exists for this tenant: " + normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(request.password());

        AppUser user = new AppUser(
                tenant,
                request.fullName().trim(),
                normalizedEmail,
                passwordHash,
                request.role()
        );

        AppUser savedUser = appUserRepository.save(user);

        auditLogService.recordSuccess(
                tenant,
                actorUser,
                savedUser,
                AuditAction.USER_CREATED,
                "User created successfully: " + normalizedEmail
        );

        return mapToResponse(savedUser);
    }

    public PageResponse<AppUserResponse> getUsersByTenant(
            UUID tenantId,
            UserRole role,
            UserStatus status,
            String search,
            Pageable pageable
    ) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found with id: " + tenantId);
        }

        String normalizedSearch = normalizeSearch(search);

        Page<AppUser> users = appUserRepository.findTenantUsers(
                tenantId,
                role,
                status,
                normalizedSearch,
                pageable
        );

        return new PageResponse<>(
                users.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isFirst(),
                users.isLast()
        );
    }

    public AppUserResponse getUserByTenantAndId(UUID tenantId, UUID userId) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        return mapToResponse(user);
    }

    public AppUserResponse updateUser(
            UUID tenantId,
            UUID userId,
            AppUserUpdateRequest request,
            Jwt jwt
    ) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        AppUser actorUser = currentActorService.getRequiredActiveActor(tenantId, jwt);

        String oldEmail = user.getEmail();
        String normalizedEmail = request.email().trim().toLowerCase();

        appUserRepository.findByTenantIdAndEmail(tenantId, normalizedEmail)
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        throw new DuplicateResourceException(
                                "User email already exists for this tenant: " + normalizedEmail
                        );
                    }
                });

        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);

        AppUser updatedUser = appUserRepository.save(user);

        auditLogService.recordSuccess(
                user.getTenant(),
                actorUser,
                updatedUser,
                AuditAction.USER_UPDATED,
                "User profile updated successfully from " + oldEmail + " to " + updatedUser.getEmail()
        );

        return mapToResponse(updatedUser);
    }

    public AppUserResponse updateUserRole(
            UUID tenantId,
            UUID userId,
            AppUserRoleUpdateRequest request,
            Jwt jwt
    ) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        AppUser actorUser = currentActorService.getRequiredActiveActor(tenantId, jwt);
        UserRole oldRole = user.getRole();

        user.setRole(request.role());

        AppUser updatedUser = appUserRepository.save(user);

        auditLogService.recordSuccess(
                user.getTenant(),
                actorUser,
                updatedUser,
                AuditAction.USER_ROLE_UPDATED,
                "User role updated successfully for " + updatedUser.getEmail()
                        + " from " + oldRole
                        + " to " + request.role()
        );

        return mapToResponse(updatedUser);
    }

    public AppUserResponse updateUserStatus(
            UUID tenantId,
            UUID userId,
            AppUserStatusUpdateRequest request,
            Jwt jwt
    ) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        AppUser actorUser = currentActorService.getRequiredActiveActor(tenantId, jwt);
        UserStatus oldStatus = user.getStatus();

        user.setStatus(request.status());

        AppUser updatedUser = appUserRepository.save(user);

        auditLogService.recordSuccess(
                user.getTenant(),
                actorUser,
                updatedUser,
                AuditAction.USER_STATUS_UPDATED,
                "User status updated successfully for " + updatedUser.getEmail()
                        + " from " + oldStatus
                        + " to " + request.status()
        );

        return mapToResponse(updatedUser);
    }

    public AppUserResponse deactivateUser(
            UUID tenantId,
            UUID userId,
            Jwt jwt
    ) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        AppUser actorUser = currentActorService.getRequiredActiveActor(tenantId, jwt);

        user.setStatus(UserStatus.INACTIVE);

        AppUser updatedUser = appUserRepository.save(user);

        auditLogService.recordSuccess(
                user.getTenant(),
                actorUser,
                updatedUser,
                AuditAction.USER_DEACTIVATED,
                "User deactivated successfully: " + updatedUser.getEmail()
        );

        return mapToResponse(updatedUser);
    }

    private AppUserResponse mapToResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getTenant().getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeSearch(String search) {
        if (search == null || search.trim().isBlank()) {
            return null;
        }

        return search.trim();
    }
}