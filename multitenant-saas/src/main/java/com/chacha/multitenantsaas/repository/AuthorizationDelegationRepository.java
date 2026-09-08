package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AuthorizationDelegation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationDelegationRepository
        extends JpaRepository<AuthorizationDelegation, UUID> {

    Optional<AuthorizationDelegation> findByTenant_IdAndId(UUID tenantId, UUID delegationId);

    Optional<AuthorizationDelegation> findByTenant_IdAndDelegatedAssignment_Id(
            UUID tenantId, UUID assignmentId);

    boolean existsByTenant_IdAndDelegatedAssignment_Id(UUID tenantId, UUID assignmentId);

    List<AuthorizationDelegation> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

    List<AuthorizationDelegation> findByTenant_IdAndDelegatorUser_IdOrderByCreatedAtDesc(
            UUID tenantId, UUID delegatorUserId);
}
