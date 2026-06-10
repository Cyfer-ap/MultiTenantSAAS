package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.DashboardSummaryResponse;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;

    public DashboardService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
    }

    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse(
                tenantRepository.count(),
                tenantRepository.countByStatus(TenantStatus.ACTIVE),
                tenantRepository.countByStatus(TenantStatus.INACTIVE),
                tenantRepository.countByStatus(TenantStatus.SUSPENDED),

                appUserRepository.count(),
                appUserRepository.countByStatus(UserStatus.ACTIVE),
                appUserRepository.countByStatus(UserStatus.INACTIVE),
                appUserRepository.countByStatus(UserStatus.SUSPENDED)
        );
    }
}