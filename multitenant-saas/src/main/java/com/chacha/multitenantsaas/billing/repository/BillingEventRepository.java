package com.chacha.multitenantsaas.billing.repository;

import com.chacha.multitenantsaas.billing.entity.BillingEvent;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingEventRepository extends JpaRepository<BillingEvent, UUID> {

    boolean existsByProviderAndProviderEventId(
            BillingProviderType provider, String providerEventId);
}
