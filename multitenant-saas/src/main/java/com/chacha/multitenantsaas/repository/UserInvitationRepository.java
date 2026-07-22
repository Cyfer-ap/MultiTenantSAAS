package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.UserInvitation;
import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserInvitationRepository
        extends JpaRepository<UserInvitation, UUID> {

    Optional<UserInvitation> findByTokenHash(String tokenHash);

    List<UserInvitation> findByTenant_IdAndEmailAndStatus(
            UUID tenantId,
            String email,
            UserInvitationStatus status
    );
}