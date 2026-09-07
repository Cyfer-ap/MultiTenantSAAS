package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TenantSubscriptionHistory;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantSubscriptionHistoryRepository
        extends JpaRepository<TenantSubscriptionHistory, UUID> {

    boolean existsBySubscriptionId(UUID subscriptionId);

    Page<TenantSubscriptionHistory> findByTenantId(UUID tenantId, Pageable pageable);
}
