package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantDashboardSummaryResponse;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.security.AuthenticatedUserContext;
import com.chacha.multitenantsaas.security.JwtContextService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantDashboardService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final JwtContextService jwtContextService;

    public TenantDashboardService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            JwtContextService jwtContextService
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.jwtContextService = jwtContextService;
    }

    public TenantDashboardSummaryResponse getTenantSummary(Jwt jwt) {
        AuthenticatedUserContext currentUser = jwtContextService.getCurrentUser(jwt);

        UUID tenantId = currentUser.tenantId();

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