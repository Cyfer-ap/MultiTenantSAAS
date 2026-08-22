package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import com.chacha.multitenantsaas.entity.TenantSubscription;
import com.chacha.multitenantsaas.entity.TenantSubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {

    boolean existsByTenant_Id(UUID tenantId);

    @Query(
            """
            SELECT subscription
            FROM TenantSubscription subscription
            JOIN FETCH subscription.tenant tenant
            JOIN FETCH subscription.plan plan
            WHERE tenant.id = :tenantId
            """)
    Optional<TenantSubscription> findByTenantIdWithPlan(@Param("tenantId") UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT subscription
            FROM TenantSubscription subscription
            JOIN FETCH subscription.tenant tenant
            JOIN FETCH subscription.plan plan
            WHERE tenant.id = :tenantId
            """)
    Optional<TenantSubscription> findByTenantIdWithPlanForUpdate(@Param("tenantId") UUID tenantId);

    @Query(
            value =
                    """
                    SELECT subscription
                    FROM TenantSubscription subscription
                    JOIN FETCH subscription.tenant tenant
                    JOIN FETCH subscription.plan plan
                    WHERE subscription.billingProvider IS NOT NULL
                      AND (:provider IS NULL OR subscription.billingProvider = :provider)
                      AND (:status IS NULL OR subscription.status = :status)
                      AND (
                          :search IS NULL
                          OR LOWER(tenant.name) LIKE LOWER(CONCAT('%', :search, '%'))
                          OR LOWER(plan.code) LIKE LOWER(CONCAT('%', :search, '%'))
                          OR LOWER(subscription.providerSubscriptionId)
                              LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                    """,
            countQuery =
                    """
                    SELECT COUNT(subscription)
                    FROM TenantSubscription subscription
                    JOIN subscription.tenant tenant
                    JOIN subscription.plan plan
                    WHERE subscription.billingProvider IS NOT NULL
                      AND (:provider IS NULL OR subscription.billingProvider = :provider)
                      AND (:status IS NULL OR subscription.status = :status)
                      AND (
                          :search IS NULL
                          OR LOWER(tenant.name) LIKE LOWER(CONCAT('%', :search, '%'))
                          OR LOWER(plan.code) LIKE LOWER(CONCAT('%', :search, '%'))
                          OR LOWER(subscription.providerSubscriptionId)
                              LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                    """)
    Page<TenantSubscription> findLinkedSubscriptions(
            @Param("provider") BillingProviderType provider,
            @Param("status") TenantSubscriptionStatus status,
            @Param("search") String search,
            Pageable pageable);
}
