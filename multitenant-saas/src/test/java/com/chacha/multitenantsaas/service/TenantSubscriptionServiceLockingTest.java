package com.chacha.multitenantsaas.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.TenantSubscriptionLifecycleUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionPlanChangeRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantSubscriptionServiceLockingTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Mock private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Mock private TenantLookupService tenantLookupService;

    @Mock private SubscriptionPlanService subscriptionPlanService;

    private TenantSubscriptionService tenantSubscriptionService;

    @BeforeEach
    void setUp() {
        tenantSubscriptionService =
                new TenantSubscriptionService(
                        tenantSubscriptionRepository, tenantLookupService, subscriptionPlanService);
    }

    @Test
    void startSubscriptionLocksTenantBeforeDuplicateCheck() {
        Tenant tenant = mock(Tenant.class);
        TenantSubscriptionStartRequest request = mock(TenantSubscriptionStartRequest.class);

        when(tenantLookupService.getActiveByIdForUpdateOrThrow(TENANT_ID)).thenReturn(tenant);
        when(tenantSubscriptionRepository.existsByTenant_Id(TENANT_ID)).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> tenantSubscriptionService.startSubscription(TENANT_ID, request));

        InOrder inOrder = inOrder(tenantLookupService, tenantSubscriptionRepository);

        inOrder.verify(tenantLookupService).getActiveByIdForUpdateOrThrow(TENANT_ID);
        inOrder.verify(tenantSubscriptionRepository).existsByTenant_Id(TENANT_ID);
    }

    @Test
    void changePlanLoadsSubscriptionWithWriteLock() {
        TenantSubscriptionPlanChangeRequest request =
                mock(TenantSubscriptionPlanChangeRequest.class);

        when(tenantSubscriptionRepository.findByTenantIdWithPlanForUpdate(TENANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> tenantSubscriptionService.changePlan(TENANT_ID, request));

        verify(tenantSubscriptionRepository).findByTenantIdWithPlanForUpdate(TENANT_ID);
        verify(tenantSubscriptionRepository, never()).findByTenantIdWithPlan(TENANT_ID);
    }

    @Test
    void updateLifecycleLoadsSubscriptionWithWriteLock() {
        TenantSubscriptionLifecycleUpdateRequest request =
                mock(TenantSubscriptionLifecycleUpdateRequest.class);

        when(tenantSubscriptionRepository.findByTenantIdWithPlanForUpdate(TENANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> tenantSubscriptionService.updateLifecycle(TENANT_ID, request));

        verify(tenantSubscriptionRepository).findByTenantIdWithPlanForUpdate(TENANT_ID);
        verify(tenantSubscriptionRepository, never()).findByTenantIdWithPlan(TENANT_ID);
    }
}
