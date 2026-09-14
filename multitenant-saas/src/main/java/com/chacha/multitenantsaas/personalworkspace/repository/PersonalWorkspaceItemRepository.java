package com.chacha.multitenantsaas.personalworkspace.repository;

import com.chacha.multitenantsaas.personalworkspace.entity.PersonalWorkspaceItem;
import com.chacha.multitenantsaas.personalworkspace.model.PersonalResourceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalWorkspaceItemRepository
        extends JpaRepository<PersonalWorkspaceItem, UUID> {

    Optional<PersonalWorkspaceItem>
            findByTenant_IdAndUser_IdAndResourceTypeAndResourceId(
                    UUID tenantId,
                    UUID userId,
                    PersonalResourceType resourceType,
                    UUID resourceId);

    List<PersonalWorkspaceItem>
            findByTenant_IdAndUser_IdAndFavoriteAtIsNotNullOrderByFavoriteAtDesc(
                    UUID tenantId, UUID userId, Pageable pageable);

    List<PersonalWorkspaceItem>
            findByTenant_IdAndUser_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(
                    UUID tenantId, UUID userId, Pageable pageable);
}
