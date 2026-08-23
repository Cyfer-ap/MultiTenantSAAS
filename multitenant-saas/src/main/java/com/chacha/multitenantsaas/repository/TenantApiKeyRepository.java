package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TenantApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantApiKeyRepository extends JpaRepository<TenantApiKey, UUID> {

    Optional<TenantApiKey> findByKeyPrefix(String keyPrefix);

    Optional<TenantApiKey> findByTenant_IdAndId(UUID tenantId, UUID id);

    List<TenantApiKey> findAllByTenant_IdOrderByCreatedAtDesc(UUID tenantId);
}
