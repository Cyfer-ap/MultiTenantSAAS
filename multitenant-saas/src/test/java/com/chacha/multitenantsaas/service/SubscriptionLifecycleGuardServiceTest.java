package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.SubscriptionAccessLevel;
import com.chacha.multitenantsaas.dto.SubscriptionAccessReason;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionEntitlementResponse.ResourceEntitlement;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException;
import com.chacha.multitenantsaas.exception.SubscriptionRestrictionException.RestrictionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionLifecycleGuardServiceTest {

    private static final UUID TENANT_ID =
            UUID.randomUUID();

    @Mock
    private SubscriptionEntitlementService entitlementService;

    private SubscriptionLifecycleGuardService guardService;

    @BeforeEach
    void setUp() {
        guardService =
                new SubscriptionLifecycleGuardService(
                        entitlementService,
                        true
                );
    }

    @Test
    void activeSubscriptionAllowsBusinessMutation() {
        when(entitlementService.evaluate(TENANT_ID))
                .thenReturn(entitlements(
                        true,
                        SubscriptionAccessReason.ACTIVE
                ));

        assertDoesNotThrow(
                () -> guardService
                        .requireBusinessMutationAllowed(
                                TENANT_ID
                        )
        );

        verify(entitlementService).evaluate(TENANT_ID);
    }

    @Test
    void pastDueGraceAllowsBusinessMutation() {
        when(entitlementService.evaluate(TENANT_ID))
                .thenReturn(entitlements(
                        true,
                        SubscriptionAccessReason
                                .PAST_DUE_GRACE
                ));

        assertDoesNotThrow(
                () -> guardService
                        .requireBusinessMutationAllowed(
                                TENANT_ID
                        )
        );
    }

    @Test
    void blockedSubscriptionProducesReadOnlyRestriction() {
        when(entitlementService.evaluate(TENANT_ID))
                .thenReturn(entitlements(
                        false,
                        SubscriptionAccessReason.CANCELLED
                ));

        SubscriptionRestrictionException exception =
                assertThrows(
                        SubscriptionRestrictionException.class,
                        () -> guardService
                                .requireBusinessMutationAllowed(
                                        TENANT_ID
                                )
                );

        assertThat(exception.getRestrictionType())
                .isEqualTo(
                        RestrictionType.WORKSPACE_READ_ONLY
                );
        assertThat(exception.getAccessReason())
                .isEqualTo(
                        SubscriptionAccessReason.CANCELLED
                );
        assertThat(exception.getResource())
                .isEqualTo("workspace");
    }

    @Test
    void disabledEnforcementDoesNotEvaluateSubscription() {
        SubscriptionLifecycleGuardService disabled =
                new SubscriptionLifecycleGuardService(
                        entitlementService,
                        false
                );

        disabled.requireBusinessMutationAllowed(TENANT_ID);

        verify(entitlementService, never())
                .evaluate(TENANT_ID);
    }

    private TenantSubscriptionEntitlementResponse entitlements(
            boolean mutationsAllowed,
            SubscriptionAccessReason accessReason
    ) {
        Instant now = Instant.now();

        ResourceEntitlement resource =
                new ResourceEntitlement(
                        0L,
                        null,
                        null,
                        true,
                        false,
                        false,
                        mutationsAllowed
                );

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
                accessReason,
                mutationsAllowed,
                mutationsAllowed,
                false,
                now.plusSeconds(3600),
                null,
                now,
                resource,
                resource
        );
    }
}
