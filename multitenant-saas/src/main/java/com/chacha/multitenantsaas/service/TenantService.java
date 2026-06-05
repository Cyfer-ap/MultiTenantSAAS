package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantCreateRequest;
import com.chacha.multitenantsaas.dto.TenantResponse;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public TenantResponse createTenant(TenantCreateRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Tenant slug already exists: " + request.slug());
        }

        Tenant tenant = new Tenant(request.name(), request.slug());

        Tenant savedTenant = tenantRepository.save(tenant);

        return mapToResponse(savedTenant);
    }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
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
}

