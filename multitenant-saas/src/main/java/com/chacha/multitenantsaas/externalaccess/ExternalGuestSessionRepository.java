package com.chacha.multitenantsaas.externalaccess;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExternalGuestSessionRepository extends JpaRepository<ExternalGuestSession, UUID> {

    Optional<ExternalGuestSession> findByTokenHash(String tokenHash);
}
