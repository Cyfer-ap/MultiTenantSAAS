package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TenantFederatedIdentity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TenantFederatedIdentityRepository
        extends JpaRepository<TenantFederatedIdentity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TenantFederatedIdentity> findByTenant_IdAndIssuerHashAndSubject(
            UUID tenantId, String issuerHash, String subject);

    Optional<TenantFederatedIdentity> findByIdentityProvider_IdAndUser_IdAndIssuerHash(
            UUID identityProviderId, UUID userId, String issuerHash);
}
