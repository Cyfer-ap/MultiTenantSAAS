package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
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

public interface AppUserRepository
        extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {

    List<AppUser> findByTenantId(UUID tenantId);

    Page<AppUser> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<AppUser> findByTenantIdAndId(UUID tenantId, UUID userId);

    @Query(
            """
            SELECT appUser
            FROM AppUser appUser
            JOIN FETCH appUser.tenant tenant
            WHERE tenant.id = :tenantId
              AND appUser.id = :userId
            """)
    Optional<AppUser> findSessionUserByTenantIdAndId(
            @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT appUser
            FROM AppUser appUser
            WHERE appUser.tenant.id = :tenantId
              AND appUser.id = :userId
            """)
    Optional<AppUser> findByTenantIdAndIdForUpdate(
            @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT appUser
            FROM AppUser appUser
            WHERE appUser.id = :userId
            """)
    Optional<AppUser> findByIdForUpdate(@Param("userId") UUID userId);

    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);

    @Query(
            """
            SELECT appUser
            FROM AppUser appUser
            JOIN FETCH appUser.tenant tenant
            WHERE appUser.email = :email
            ORDER BY tenant.name
            """)
    List<AppUser> findByEmailWithTenant(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT appUser
            FROM AppUser appUser
            WHERE appUser.tenant.id = :tenantId
              AND appUser.email = :email
            """)
    Optional<AppUser> findByTenantIdAndEmailForUpdate(
            @Param("tenantId") UUID tenantId, @Param("email") String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    long countByStatus(UserStatus status);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, UserStatus status);

    long countByTenantIdAndRoleAndStatus(UUID tenantId, UserRole role, UserStatus status);

    default Page<AppUser> findTenantUsers(
            UUID tenantId, UserRole role, UserStatus status, String search, Pageable pageable) {
        Specification<AppUser> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate = criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);

                    if (role != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate, criteriaBuilder.equal(root.get("role"), role));
                    }

                    if (status != null) {
                        predicate =
                                criteriaBuilder.and(
                                        predicate,
                                        criteriaBuilder.equal(root.get("status"), status));
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
