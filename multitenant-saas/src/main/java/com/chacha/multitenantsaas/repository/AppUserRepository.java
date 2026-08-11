package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
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

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    List<AppUser> findByTenantId(UUID tenantId);

    Page<AppUser> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<AppUser> findByTenantIdAndId(UUID tenantId, UUID userId);

    @Query("""
            SELECT appUser
            FROM AppUser appUser
            JOIN FETCH appUser.tenant tenant
            WHERE tenant.id = :tenantId
              AND appUser.id = :userId
            """)
    Optional<AppUser> findSessionUserByTenantIdAndId(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT appUser
            FROM AppUser appUser
            WHERE appUser.tenant.id = :tenantId
              AND appUser.id = :userId
            """)
    Optional<AppUser> findByTenantIdAndIdForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT appUser
            FROM AppUser appUser
            WHERE appUser.id = :userId
            """)
    Optional<AppUser> findByIdForUpdate(
            @Param("userId") UUID userId
    );

    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT appUser
            FROM AppUser appUser
            WHERE appUser.tenant.id = :tenantId
              AND appUser.email = :email
            """)
    Optional<AppUser> findByTenantIdAndEmailForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("email") String email
    );

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    long countByStatus(UserStatus status);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, UserStatus status);

    long countByTenantIdAndRoleAndStatus(UUID tenantId, UserRole role, UserStatus status);

    @Query("""
            SELECT appUser
            FROM AppUser appUser
            WHERE appUser.tenant.id = :tenantId
              AND (:role IS NULL OR appUser.role = :role)
              AND (:status IS NULL OR appUser.status = :status)
              AND (
                    :search IS NULL
                    OR LOWER(appUser.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(appUser.email) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<AppUser> findTenantUsers(
            @Param("tenantId") UUID tenantId,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}