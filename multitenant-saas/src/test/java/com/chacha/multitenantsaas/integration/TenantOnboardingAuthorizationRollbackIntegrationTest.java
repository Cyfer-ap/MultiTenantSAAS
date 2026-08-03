package com.chacha.multitenantsaas.integration;

import com.chacha.multitenantsaas.dto.TenantOnboardingRequest;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import com.chacha.multitenantsaas.service.AuthorizationProvisioningService;
import com.chacha.multitenantsaas.service.TenantOnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class TenantOnboardingAuthorizationRollbackIntegrationTest {

    @Autowired
    private TenantOnboardingService
            tenantOnboardingService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @MockitoBean
    private AuthorizationProvisioningService
            authorizationProvisioningService;

    @Test
    void provisioningFailureRollsBackTenantAndUser() {
        String suffix =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        String slug =
                "rollback-" + suffix;

        long tenantCountBefore =
                tenantRepository.count();

        long userCountBefore =
                appUserRepository.count();

        when(
                authorizationProvisioningService
                        .provisionInitialTenantAdministrator(
                                any(UUID.class),
                                any(UUID.class)
                        )
        )
                .thenThrow(
                        new IllegalStateException(
                                "Simulated authorization "
                                        + "provisioning failure"
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tenantOnboardingService
                                .onboardTenant(
                                        new TenantOnboardingRequest(
                                                "Rollback Tenant",
                                                slug,
                                                "Rollback Administrator",
                                                "rollback.admin."
                                                        + suffix
                                                        + "@example.test",
                                                "StrongPassword@123"
                                        )
                                )
        );

        assertFalse(
                tenantRepository.existsBySlug(slug)
        );

        assertEquals(
                tenantCountBefore,
                tenantRepository.count()
        );

        assertEquals(
                userCountBefore,
                appUserRepository.count()
        );
    }
}