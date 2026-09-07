package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantIdentityProviderRepository
        extends JpaRepository<TenantIdentityProvider, UUID> {

    Optional<TenantIdentityProvider> findByTenant_Id(UUID tenantId);

    boolean existsByTenant_Id(UUID tenantId);
}
