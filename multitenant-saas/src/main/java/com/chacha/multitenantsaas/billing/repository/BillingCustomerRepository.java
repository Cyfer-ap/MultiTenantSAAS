package com.chacha.multitenantsaas.billing.repository;

import com.chacha.multitenantsaas.billing.entity.BillingCustomer;
import com.chacha.multitenantsaas.billing.provider.BillingProviderType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingCustomerRepository extends JpaRepository<BillingCustomer, UUID> {

    Optional<BillingCustomer> findByTenantIdAndProvider(
            UUID tenantId, BillingProviderType provider);
}
