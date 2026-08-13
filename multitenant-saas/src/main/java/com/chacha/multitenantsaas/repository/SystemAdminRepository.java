package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import jakarta.persistence.LockModeType;
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

public interface SystemAdminRepository
        extends JpaRepository<SystemAdmin, UUID>, JpaSpecificationExecutor<SystemAdmin> {

    Optional<SystemAdmin> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT systemAdmin
            FROM SystemAdmin systemAdmin
            WHERE systemAdmin.id = :systemAdminId
            """)
    Optional<SystemAdmin> findByIdForUpdate(@Param("systemAdminId") UUID systemAdminId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT systemAdmin
            FROM SystemAdmin systemAdmin
            WHERE systemAdmin.email = :email
            """)
    Optional<SystemAdmin> findByEmailForUpdate(@Param("email") String email);

    boolean existsByEmail(String email);

    long countByStatus(UserStatus status);

    default Page<SystemAdmin> findSystemAdmins(
            UserStatus status, String search, Pageable pageable) {
        Specification<SystemAdmin> specification =
                (root, query, criteriaBuilder) -> {
                    var predicate = criteriaBuilder.conjunction();

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
