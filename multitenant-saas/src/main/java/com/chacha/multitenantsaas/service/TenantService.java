package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantCreateRequest;
import com.chacha.multitenantsaas.dto.TenantResponse;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.stereotype.Service;
import com.chacha.multitenantsaas.dto.TenantUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantStatusUpdateRequest;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public TenantResponse createTenant(TenantCreateRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Tenant slug already exists: " + request.slug());
        }

        Tenant tenant = new Tenant(request.name(), request.slug());

        Tenant savedTenant = tenantRepository.save(tenant);

        return mapToResponse(savedTenant);
    }

    public PageResponse<TenantResponse> getAllTenants(Pageable pageable) {
        Page<Tenant> tenants = tenantRepository.findAll(pageable);

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
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));

        return mapToResponse(tenant);
    }

    public TenantResponse getTenantBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with slug: " + slug));

        return mapToResponse(tenant);
    }

    public TenantResponse updateTenant(UUID id, TenantUpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));

        tenantRepository.findBySlug(request.slug())
                .ifPresent(existingTenant -> {
                    if (!existingTenant.getId().equals(id)) {
                        throw new DuplicateResourceException("Tenant slug already exists: " + request.slug());
                    }
                });

        tenant.setName(request.name());
        tenant.setSlug(request.slug());

        Tenant updatedTenant = tenantRepository.save(tenant);

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

    public TenantResponse updateTenantStatus(UUID id, TenantStatusUpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));

        tenant.setStatus(request.status());

        Tenant updatedTenant = tenantRepository.save(tenant);

        return mapToResponse(updatedTenant);
    }

    public TenantResponse deactivateTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + id));

        tenant.setStatus(TenantStatus.INACTIVE);

        Tenant updatedTenant = tenantRepository.save(tenant);

        return mapToResponse(updatedTenant);
    }
}

