package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.SubscriptionAccessLevel;
import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse.ResourceEntitlement;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException.RestrictionType;
import com.chacha.multitenantsaas.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionQuotaGuardServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Mock private SubscriptionEntitlementService entitlementService;

    @Mock private TenantSubscriptionRepository tenantSubscriptionRepository;

    private SubscriptionQuotaGuardService guardService;

    @BeforeEach
    void setUp() {
        guardService =
                new SubscriptionQuotaGuardService(
                        entitlementService, tenantSubscriptionRepository, true);
    }

    @Test
    void allowsUserCreationWhenCapacityRemains() {
        when(tenantSubscriptionRepository.findByTenantIdWithPlanForUpdate(TENANT_ID))
                .thenReturn(Optional.empty());
        when(entitlementService.evaluate(TENANT_ID))
                .thenReturn(
                        entitlements(
                                true,
                                SubscriptionAccessReason.ACTIVE,
                                resource(2L, 3L, true),
                                resource(1L, 5L, true)));

        guardService.requireUserSlot(TENANT_ID);

        verify(tenantSubscriptionRepository).findByTenantIdWithPlanForUpdate(TENANT_ID);
        verify(entitlementService).evaluate(TENANT_ID);
    }

    @Test
    void rejectsUserCreationWhenLimitIsReached() {
        when(tenantSubscriptionRepository.findByTenantIdWithPlanForUpdate(TENANT_ID))
                .thenReturn(Optional.empty());
        when(entitlementService.evaluate(TENANT_ID))
                .thenReturn(
                        entitlements(
                                true,
                                SubscriptionAccessReason.ACTIVE,
                                resource(3L, 3L, false),
                                resource(0L, 5L, true)));

        SubscriptionRestrictionException exception =
                assertThrows(
                        SubscriptionRestrictionException.class,
                        () -> guardService.requireUserSlot(TENANT_ID));

        assertThat(exception.getRestrictionType()).isEqualTo(RestrictionType.USER_LIMIT_REACHED);
        assertThat(exception.getResource()).isEqualTo("users");
        assertThat(exception.getUsed()).isEqualTo(3L);
        assertThat(exception.getLimit()).isEqualTo(3L);
    }

    @Test
    void rejectsProjectCreationWhenLimitIsReached() {
        when(tenantSubscriptionRepository.findByTenantIdWithPlanForUpdate(TENANT_ID))
                .thenReturn(Optional.empty());
        when(entitlementService.evaluate(TENANT_ID))
                .thenReturn(
                        entitlements(
                                true,
                                SubscriptionAccessReason.ACTIVE,
                                resource(1L, 5L, true),
                                resource(2L, 2L, false)));

        SubscriptionRestrictionException exception =
                assertThrows(
                        SubscriptionRestrictionException.class,
                        () -> guardService.requireProjectSlot(TENANT_ID));

        assertThat(exception.getRestrictionType()).isEqualTo(RestrictionType.PROJECT_LIMIT_REACHED);
    }

    @Test
    void rejectsGrowthWhenSubscriptionMutationsAreBlocked() {
        when(tenantSubscriptionRepository.findByTenantIdWithPlanForUpdate(TENANT_ID))
                .thenReturn(Optional.empty());
        when(entitlementService.evaluate(TENANT_ID))
                .thenReturn(
                        entitlements(
                                false,
                                SubscriptionAccessReason.CANCELLED,
                                resource(1L, 5L, false),
                                resource(1L, 5L, false)));

        SubscriptionRestrictionException exception =
                assertThrows(
                        SubscriptionRestrictionException.class,
                        () -> guardService.requireProjectSlot(TENANT_ID));

        assertThat(exception.getRestrictionType()).isEqualTo(RestrictionType.SERVICE_UNAVAILABLE);
        assertThat(exception.getAccessReason()).isEqualTo(SubscriptionAccessReason.CANCELLED);
    }

    @Test
    void disabledEnforcementDoesNotEvaluateOrLock() {
        SubscriptionQuotaGuardService disabled =
                new SubscriptionQuotaGuardService(
                        entitlementService, tenantSubscriptionRepository, false);

        disabled.requireUserSlot(TENANT_ID);

        verify(tenantSubscriptionRepository, never()).findByTenantIdWithPlanForUpdate(TENANT_ID);
        verify(entitlementService, never()).evaluate(TENANT_ID);
    }

    private TenantSubscriptionEntitlementResponse entitlements(
            boolean mutationsAllowed,
            SubscriptionAccessReason reason,
            ResourceEntitlement users,
            ResourceEntitlement projects) {
        Instant now = Instant.now();

        return new TenantSubscriptionEntitlementResponse(
                TENANT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TEST",
                "Test",
                TenantSubscriptionStatus.ACTIVE,
                mutationsAllowed
                        ? SubscriptionAccessLevel.FULL_ACCESS
                        : SubscriptionAccessLevel.BLOCKED,
                reason,
                mutationsAllowed,
                mutationsAllowed,
                false,
                now.plusSeconds(3600),
                null,
                now,
                users,
                projects);
    }

    private ResourceEntitlement resource(long used, Long limit, boolean creationAllowed) {
        boolean unlimited = limit == null;
        boolean reached = limit != null && used >= limit;
        boolean over = limit != null && used > limit;
        Long remaining = limit == null ? null : Math.max(0L, limit - used);

        return new ResourceEntitlement(
                used, limit, remaining, unlimited, reached, over, creationAllowed);
    }
}
