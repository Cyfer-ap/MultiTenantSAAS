package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TenantFederatedIdentity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantFederatedIdentityRepository
        extends JpaRepository<TenantFederatedIdentity, UUID> {

    Optional<TenantFederatedIdentity> findByTenant_IdAndIssuerHashAndSubject(
            UUID tenantId, String issuerHash, String subject);

    Optional<TenantFederatedIdentity> findByIdentityProvider_IdAndUser_IdAndIssuerHash(
            UUID identityProviderId, UUID userId, String issuerHash);
}
