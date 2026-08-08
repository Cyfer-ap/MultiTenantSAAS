package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SystemAdminRepository extends JpaRepository<SystemAdmin, UUID> {

    Optional<SystemAdmin> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT systemAdmin
            FROM SystemAdmin systemAdmin
            WHERE systemAdmin.email = :email
            """)
    Optional<SystemAdmin> findByEmailForUpdate(
            @Param("email") String email
    );

    boolean existsByEmail(String email);

    long countByStatus(UserStatus status);

    @Query("""
            SELECT systemAdmin
            FROM SystemAdmin systemAdmin
            WHERE (:status IS NULL OR systemAdmin.status = :status)
              AND (
                    :search IS NULL
                    OR LOWER(systemAdmin.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(systemAdmin.email) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<SystemAdmin> findSystemAdmins(
            @Param("status") UserStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}