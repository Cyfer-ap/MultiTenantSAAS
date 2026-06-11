package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantDashboardSummaryResponse;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantDashboardService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;

    public TenantDashboardService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
    }

    public TenantDashboardSummaryResponse getTenantSummary(Jwt jwt) {
        UUID tenantId;

        try {
            tenantId = UUID.fromString(jwt.getClaimAsString("tenantId"));
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationFailedException("Invalid token data");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AuthenticationFailedException("Tenant not found"));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new AuthenticationFailedException("Tenant is not active");
        }

        return new TenantDashboardSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),

                appUserRepository.countByTenantId(tenantId),
                appUserRepository.countByTenantIdAndStatus(tenantId, UserStatus.ACTIVE),
                appUserRepository.countByTenantIdAndStatus(tenantId, UserStatus.INACTIVE),
                appUserRepository.countByTenantIdAndStatus(tenantId, UserStatus.SUSPENDED)
        );
    }
}