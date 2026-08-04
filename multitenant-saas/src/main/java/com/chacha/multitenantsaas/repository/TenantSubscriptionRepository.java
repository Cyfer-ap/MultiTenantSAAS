package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantSubscriptionRepository
        extends JpaRepository<TenantSubscription, UUID> {

    boolean existsByTenant_Id(UUID tenantId);

    @Query("""
            SELECT subscription
            FROM TenantSubscription subscription
            JOIN FETCH subscription.tenant tenant
            JOIN FETCH subscription.plan plan
            WHERE tenant.id = :tenantId
            """)
    Optional<TenantSubscription> findByTenantIdWithPlan(
            @Param("tenantId") UUID tenantId
    );
}
