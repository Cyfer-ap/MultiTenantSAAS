package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByStatus(TenantStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT tenant
            FROM Tenant tenant
            WHERE tenant.id = :tenantId
            """)
    Optional<Tenant> findByIdForUpdate(@Param("tenantId") UUID tenantId);

    @Query(
            """
            SELECT tenant
            FROM Tenant tenant
            WHERE (:status IS NULL OR tenant.status = :status)
              AND (
                    :search IS NULL
                    OR LOWER(tenant.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(tenant.slug) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Tenant> findTenants(
            @Param("status") TenantStatus status,
            @Param("search") String search,
            Pageable pageable);
}
