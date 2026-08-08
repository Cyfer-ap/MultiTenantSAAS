package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.UserInvitation;
import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserInvitationRepository
        extends JpaRepository<UserInvitation, UUID> {

    Optional<UserInvitation> findByTokenHash(String tokenHash);

    Optional<UserInvitation> findByTenant_IdAndId(
            UUID tenantId,
            UUID invitationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT invitation
            FROM UserInvitation invitation
            WHERE invitation.tokenHash = :tokenHash
            """)
    Optional<UserInvitation> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT invitation
            FROM UserInvitation invitation
            WHERE invitation.tenant.id = :tenantId
              AND invitation.id = :invitationId
            """)
    Optional<UserInvitation> findByTenantIdAndIdForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("invitationId") UUID invitationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT invitation
            FROM UserInvitation invitation
            WHERE invitation.tenant.id = :tenantId
              AND invitation.email = :email
              AND invitation.status = :status
            """)
    List<UserInvitation> findByTenantIdAndEmailAndStatusForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("email") String email,
            @Param("status") UserInvitationStatus status
    );

    @Query("""
            SELECT invitation
            FROM UserInvitation invitation
            WHERE invitation.tenant.id = :tenantId
              AND (:status IS NULL OR invitation.status = :status)
              AND (:role IS NULL OR invitation.role = :role)
              AND (
                    :search IS NULL
                    OR LOWER(invitation.fullName)
                        LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(invitation.email)
                        LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<UserInvitation> findTenantInvitations(
            @Param("tenantId") UUID tenantId,
            @Param("status") UserInvitationStatus status,
            @Param("role") UserRole role,
            @Param("search") String search,
            Pageable pageable
    );
}