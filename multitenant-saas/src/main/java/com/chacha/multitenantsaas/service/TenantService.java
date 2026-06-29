package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.dto.TenantCreateRequest;
import com.chacha.multitenantsaas.dto.TenantResponse;
import com.chacha.multitenantsaas.dto.TenantStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;
    private final CurrentActorService currentActorService;
    private final TenantLookupService tenantLookupService;
    private final RefreshTokenService refreshTokenService;

    public TenantService(
            TenantRepository tenantRepository,
            AuditLogService auditLogService,
            CurrentActorService currentActorService,
            TenantLookupService tenantLookupService,
            RefreshTokenService refreshTokenService
    ) {
        this.tenantRepository = tenantRepository;
        this.auditLogService = auditLogService;
        this.currentActorService = currentActorService;
        this.tenantLookupService = tenantLookupService;
        this.refreshTokenService = refreshTokenService;
    }

    public TenantResponse createTenant(TenantCreateRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Tenant slug already exists: " + request.slug());
        }

        Tenant tenant = new Tenant(request.name(), request.slug());

        Tenant savedTenant = tenantRepository.save(tenant);

        return mapToResponse(savedTenant);
    }

    public PageResponse<TenantResponse> getAllTenants(
            TenantStatus status,
            String search,
            Pageable pageable
    ) {
        String normalizedSearch = normalizeSearch(search);

        Page<Tenant> tenants = tenantRepository.findTenants(
                status,
                normalizedSearch,
                pageable
        );

        return new PageResponse<>(
                tenants.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList(),
                tenants.getNumber(),
                tenants.getSize(),
                tenants.getTotalElements(),
                tenants.getTotalPages(),
                tenants.isFirst(),
                tenants.isLast()
        );
    }

    public TenantResponse getTenantById(UUID id) {
        Tenant tenant = tenantLookupService.getByIdOrThrow(id);

        return mapToResponse(tenant);
    }

    public TenantResponse getTenantBySlug(String slug) {
        Tenant tenant = tenantLookupService.getBySlugOrThrow(slug);

        return mapToResponse(tenant);
    }

    public TenantResponse updateTenant(UUID id, TenantUpdateRequest request, Jwt jwt) {
        Tenant tenant = tenantLookupService.getByIdOrThrow(id);

        AppUser actorUser = currentActorService.getRequiredActiveActor(id, jwt);

        String oldName = tenant.getName();
        String oldSlug = tenant.getSlug();

        tenantRepository.findBySlug(request.slug())
                .ifPresent(existingTenant -> {
                    if (!existingTenant.getId().equals(id)) {
                        throw new DuplicateResourceException("Tenant slug already exists: " + request.slug());
                    }
                });

        tenant.setName(request.name());
        tenant.setSlug(request.slug());

        Tenant updatedTenant = tenantRepository.save(tenant);

        auditLogService.recordSuccess(
                updatedTenant,
                actorUser,
                null,
                AuditAction.TENANT_UPDATED,
                "Tenant updated successfully from name=" + oldName
                        + ", slug=" + oldSlug
                        + " to name=" + updatedTenant.getName()
                        + ", slug=" + updatedTenant.getSlug()
        );

        return mapToResponse(updatedTenant);
    }

    public TenantResponse updateTenantStatus(UUID id, TenantStatusUpdateRequest request, Jwt jwt) {
        Tenant tenant = tenantLookupService.getByIdOrThrow(id);

        AppUser actorUser = currentActorService.getRequiredActiveActor(id, jwt);

        TenantStatus oldStatus = tenant.getStatus();

        tenant.setStatus(request.status());

        Tenant updatedTenant = tenantRepository.save(tenant);

        if (updatedTenant.getStatus() != TenantStatus.ACTIVE) {
            refreshTokenService.revokeAllActiveTokensForTenant(updatedTenant.getId());
        }

        auditLogService.recordSuccess(
                updatedTenant,
                actorUser,
                null,
                AuditAction.TENANT_STATUS_UPDATED,
                "Tenant status updated successfully from " + oldStatus
                        + " to " + updatedTenant.getStatus()
        );

        return mapToResponse(updatedTenant);
    }

    public TenantResponse deactivateTenant(UUID id, Jwt jwt) {
        Tenant tenant = tenantLookupService.getByIdOrThrow(id);

        AppUser actorUser = currentActorService.getRequiredActiveActor(id, jwt);

        TenantStatus oldStatus = tenant.getStatus();

        tenant.setStatus(TenantStatus.INACTIVE);

        Tenant updatedTenant = tenantRepository.save(tenant);

        refreshTokenService.revokeAllActiveTokensForTenant(updatedTenant.getId());

        auditLogService.recordSuccess(
                updatedTenant,
                actorUser,
                null,
                AuditAction.TENANT_DEACTIVATED,
                "Tenant deactivated successfully from " + oldStatus
                        + " to " + updatedTenant.getStatus()
        );

        return mapToResponse(updatedTenant);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

    private String normalizeSearch(String search) {
        if (search == null || search.trim().isBlank()) {
            return null;
        }

        return search.trim();
    }
}