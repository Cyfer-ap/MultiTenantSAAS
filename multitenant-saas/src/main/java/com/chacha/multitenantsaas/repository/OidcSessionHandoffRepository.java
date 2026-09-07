package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.OidcSessionHandoff;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OidcSessionHandoffRepository extends JpaRepository<OidcSessionHandoff, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select handoff from OidcSessionHandoff handoff where handoff.codeHash = :codeHash")
    Optional<OidcSessionHandoff> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
