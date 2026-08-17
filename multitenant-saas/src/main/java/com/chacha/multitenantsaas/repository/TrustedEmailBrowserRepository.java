package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TrustedEmailBrowser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedEmailBrowserRepository extends JpaRepository<TrustedEmailBrowser, UUID> {

    Optional<TrustedEmailBrowser> findByTokenHash(String tokenHash);
}
