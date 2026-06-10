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


import java.util.List;
import java.util.UUID;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;

    public AppUserService(
            AppUserRepository appUserRepository,
            TenantRepository tenantRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
    }

    public AppUserResponse createUser(UUID tenantId, AppUserCreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));

        String normalizedEmail = request.email().trim().toLowerCase();

        if (appUserRepository.existsByTenantIdAndEmail(tenantId, normalizedEmail)) {
            throw new DuplicateResourceException("User email already exists for this tenant: " + normalizedEmail);
        }

        AppUser user = new AppUser(
                tenant,
                request.fullName().trim(),
                normalizedEmail,
                request.role()
        );

        AppUser savedUser = appUserRepository.save(user);

        return mapToResponse(savedUser);
    }

    public List<AppUserResponse> getUsersByTenant(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found with id: " + tenantId);
        }

        return appUserRepository.findByTenantId(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
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