package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    List<AppUser> findByTenantId(UUID tenantId);

    Optional<AppUser> findByTenantIdAndId(UUID tenantId, UUID userId);

    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}