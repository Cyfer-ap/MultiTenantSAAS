package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.UserInvitation;
import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<UserInvitation> findByTenant_IdAndEmailAndStatus(
            UUID tenantId,
            String email,
            UserInvitationStatus status
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