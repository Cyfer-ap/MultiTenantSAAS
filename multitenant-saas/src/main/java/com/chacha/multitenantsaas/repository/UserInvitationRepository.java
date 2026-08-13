package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.UserInvitation;
import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserInvitationRepository
        extends JpaRepository<UserInvitation, UUID>, JpaSpecificationExecutor<UserInvitation> {

    Optional<UserInvitation> findByTokenHash(String tokenHash);

    Optional<UserInvitation> findByTenant_IdAndId(UUID tenantId, UUID invitationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT invitation
            FROM UserInvitation invitation
            WHERE invitation.tokenHash = :tokenHash
            """)
    Optional<UserInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT invitation
            FROM UserInvitation invitation
            WHERE invitation.tenant.id = :tenantId
              AND invitation.id = :invitationId
            """)
    Optional<UserInvitation> findByTenantIdAndIdForUpdate(
            @Param("tenantId") UUID tenantId, @Param("invitationId") UUID invitationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT invitation
            FROM UserInvitation invitation
            WHERE invitation.tenant.id = :tenantId
              AND invitation.email = :email
              AND invitation.status = :status
            """)
    List<UserInvitation> findByTenantIdAndEmailAndStatusForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("email") String email,
            @Param("status") UserInvitationStatus status);

    default Page<UserInvitation> findTenantInvitations(
            UUID tenantId,
            UserInvitationStatus status,
            UserRole role,
            String search,
            Pageable pageable) {
        Specification<UserInvitation> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate = criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);

                    if (status != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.equal(root.get("status"), status));
                    }

                    if (role != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate, criteriaBuilder.equal(root.get("role"), role));
                    }

                    if (search != null) {
                        String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.or(
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                root.<String>get("fullName")),
                                                        pattern),
                                                criteriaBuilder.like(
                                                        criteriaBuilder.lower(
                                                                root.<String>get("email")),
                                                        pattern)));
                    }

                    return predicate;
                };

        return findAll(specification, pageable);
    }
}
