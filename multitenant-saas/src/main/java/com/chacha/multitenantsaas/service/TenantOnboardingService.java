package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.AppUserResponse;
import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.dto.TenantOnboardingResponse;
import com.chacha.multitenantsaas.dto.TenantResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantOnboardingService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CurrentSystemAdminService currentSystemAdminService;

    public TenantOnboardingService(
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            CurrentSystemAdminService currentSystemAdminService
    ) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.currentSystemAdminService = currentSystemAdminService;
    }

    @Transactional
    public TenantOnboardingResponse onboardTenant(TenantOnboardingRequest request) {
        String normalizedSlug = normalizeSlug(request.tenantSlug());
        String normalizedEmail = normalizeEmail(request.adminEmail());

        if (tenantRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateResourceException(
                    "Tenant already exists with slug: " + normalizedSlug
            );
        }

        Tenant savedTenant = createTenant(request, normalizedSlug);
        AppUser savedAdminUser = createInitialTenantAdmin(request, savedTenant, normalizedEmail);

        auditLogService.recordSelfSuccess(
                savedTenant,
                savedAdminUser,
                AuditAction.TENANT_ONBOARDED,
                "Tenant onboarded successfully with initial tenant administrator: " + normalizedEmail
        );

        return new TenantOnboardingResponse(
                mapTenantToResponse(savedTenant),
                mapUserToResponse(savedAdminUser),
                "Tenant onboarded successfully"
        );
    }

    @Transactional
    public TenantOnboardingResponse onboardTenantBySystemAdmin(
            TenantOnboardingRequest request,
            Jwt jwt
    ) {
        SystemAdmin actorSystemAdmin = currentSystemAdminService.getRequiredActiveSystemAdmin(jwt);

        String normalizedSlug = normalizeSlug(request.tenantSlug());
        String normalizedEmail = normalizeEmail(request.adminEmail());

        if (tenantRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateResourceException(
                    "Tenant already exists with slug: " + normalizedSlug
            );
        }

        Tenant savedTenant = createTenant(request, normalizedSlug);
        AppUser savedAdminUser = createInitialTenantAdmin(request, savedTenant, normalizedEmail);

        auditLogService.recordSystemAdminSuccess(
                savedTenant,
                actorSystemAdmin,
                savedAdminUser,
                AuditAction.TENANT_ONBOARDED,
                "Tenant onboarded successfully by system admin with initial tenant administrator: "
                        + normalizedEmail
        );

        return new TenantOnboardingResponse(
                mapTenantToResponse(savedTenant),
                mapUserToResponse(savedAdminUser),
                "Tenant onboarded successfully by system admin"
        );
    }

    private Tenant createTenant(
            TenantOnboardingRequest request,
            String normalizedSlug
    ) {
        Tenant tenant = new Tenant();

        tenant.setName(request.tenantName().trim());
        tenant.setSlug(normalizedSlug);
        tenant.setStatus(TenantStatus.ACTIVE);

        return tenantRepository.save(tenant);
    }

    private AppUser createInitialTenantAdmin(
            TenantOnboardingRequest request,
            Tenant savedTenant,
            String normalizedEmail
    ) {
        AppUser adminUser = new AppUser();

        adminUser.setTenant(savedTenant);
        adminUser.setFullName(request.adminFullName().trim());
        adminUser.setEmail(normalizedEmail);
        adminUser.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        adminUser.setRole(UserRole.TENANT_ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        return appUserRepository.save(adminUser);
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private TenantResponse mapTenantToResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

    private AppUserResponse mapUserToResponse(AppUser user) {
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
}