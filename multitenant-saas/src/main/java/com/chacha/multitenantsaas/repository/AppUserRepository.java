package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    List<AppUser> findByTenantId(UUID tenantId);

    Page<AppUser> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<AppUser> findByTenantIdAndId(UUID tenantId, UUID userId);

    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    long countByStatus(UserStatus status);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, UserStatus status);
}