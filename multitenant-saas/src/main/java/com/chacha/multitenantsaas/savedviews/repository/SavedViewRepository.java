package com.chacha.multitenantsaas.savedviews.repository;

import com.chacha.multitenantsaas.savedviews.entity.SavedView;
import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedViewRepository extends JpaRepository<SavedView, UUID> {

    List<SavedView> findByTenant_IdAndUser_IdAndTargetAndContextIdOrderByNameAsc(
            UUID tenantId,
            UUID userId,
            SavedViewTarget target,
            UUID contextId,
            Pageable pageable);

    long countByTenant_IdAndUser_IdAndTargetAndContextId(
            UUID tenantId, UUID userId, SavedViewTarget target, UUID contextId);

    Optional<SavedView> findByTenant_IdAndUser_IdAndId(UUID tenantId, UUID userId, UUID id);
}
