package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OidcAuthorizationTransaction;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OidcAuthorizationTransactionRepository
        extends JpaRepository<OidcAuthorizationTransaction, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT transaction
            FROM OidcAuthorizationTransaction transaction
            JOIN FETCH transaction.tenant
            JOIN FETCH transaction.identityProvider
            WHERE transaction.stateHash = :stateHash
            """)
    Optional<OidcAuthorizationTransaction> findByStateHashForUpdate(
            @Param("stateHash") String stateHash);
}
