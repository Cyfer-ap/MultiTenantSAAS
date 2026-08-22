package com.chacha.multitenantsaas.billing.repository;

import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingEventRepository extends JpaRepository<BillingEvent, UUID> {

    boolean existsByProviderAndProviderEventId(
            BillingProviderType provider, String providerEventId);

    @Query(
            """
            SELECT event
            FROM BillingEvent event
            WHERE (:provider IS NULL OR event.provider = :provider)
              AND (:eventType IS NULL OR LOWER(event.eventType) = LOWER(:eventType))
              AND (
                  :search IS NULL
                  OR LOWER(event.providerEventId) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(event.eventType) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<BillingEvent> findBillingEvents(
            @Param("provider") BillingProviderType provider,
            @Param("eventType") String eventType,
            @Param("search") String search,
            Pageable pageable);
}
