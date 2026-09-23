package com.chacha.multitenantsaas.externalaccess;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ExternalAccessGrantRepository extends JpaRepository<ExternalAccessGrant, UUID> {

    Optional<ExternalAccessGrant> findByTenantIdAndProjectIdAndId(
            UUID tenantId, UUID projectId, UUID grantId);

    Page<ExternalAccessGrant> findByTenantIdAndProjectIdOrderByCreatedAtDesc(
            UUID tenantId, UUID projectId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT grant
            FROM ExternalAccessGrant grant
            WHERE grant.invitationTokenHash = :tokenHash
            """)
    Optional<ExternalAccessGrant> findByInvitationTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT grant
            FROM ExternalAccessGrant grant
            WHERE grant.tenantId = :tenantId
              AND grant.projectId = :projectId
              AND grant.id = :grantId
            """)
    Optional<ExternalAccessGrant> findForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("grantId") UUID grantId);
}
