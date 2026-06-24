package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AppUserCreateRequest;
import com.chacha.multitenantsaas.dto.AppUserResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.stereotype.Service;
import com.chacha.multitenantsaas.dto.AppUserRoleUpdateRequest;
import com.chacha.multitenantsaas.dto.AppUserStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.AppUserUpdateRequest;
import com.chacha.multitenantsaas.entity.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.chacha.multitenantsaas.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;

import java.util.List;
import java.util.UUID;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(
            AppUserRepository appUserRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUserResponse createUser(UUID tenantId, AppUserCreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

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

        return mapToResponse(savedUser);
    }

    public PageResponse<AppUserResponse> getUsersByTenant(
            UUID tenantId,
            UserRole role,
            UserStatus status,
            Pageable pageable
    ) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found with id: " + tenantId);
        }

        Page<AppUser> users = appUserRepository.findTenantUsers(
                tenantId,
                role,
                status,
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

    public AppUserResponse getUserByTenantAndId(UUID tenantId, UUID userId) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        return mapToResponse(user);
    }

    public AppUserResponse updateUserRole(
            UUID tenantId,
            UUID userId,
            AppUserRoleUpdateRequest request
    ) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        user.setRole(request.role());

        AppUser updatedUser = appUserRepository.save(user);

        return mapToResponse(updatedUser);
    }

    public AppUserResponse updateUserStatus(
            UUID tenantId,
            UUID userId,
            AppUserStatusUpdateRequest request
    ) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        user.setStatus(request.status());

        AppUser updatedUser = appUserRepository.save(user);

        return mapToResponse(updatedUser);
    }

    public AppUserResponse updateUser(
            UUID tenantId,
            UUID userId,
            AppUserUpdateRequest request
    ) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

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

        return mapToResponse(updatedUser);
    }

    public AppUserResponse deactivateUser(UUID tenantId, UUID userId) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId + " for tenant: " + tenantId
                ));

        user.setStatus(UserStatus.INACTIVE);

        AppUser updatedUser = appUserRepository.save(user);

        return mapToResponse(updatedUser);
    }


}