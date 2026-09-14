package com.chacha.multitenantsaas.personalworkspace.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.personalworkspace.dto.PersonalWorkspaceItemResponse;
import com.chacha.multitenantsaas.personalworkspace.dto.PersonalWorkspaceOverviewResponse;
import com.chacha.multitenantsaas.personalworkspace.entity.PersonalWorkspaceItem;
import com.chacha.multitenantsaas.personalworkspace.model.PersonalResourceType;
import com.chacha.multitenantsaas.personalworkspace.model.PersonalWorkspaceResource;
import com.chacha.multitenantsaas.personalworkspace.repository.PersonalWorkspaceItemRepository;
import com.chacha.multitenantsaas.personalworkspace.spi.PersonalWorkspaceResourceResolver;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalWorkspaceService {

    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 50;

    private final PersonalWorkspaceItemRepository itemRepository;
    private final Map<PersonalResourceType, PersonalWorkspaceResourceResolver> resolvers;

    public PersonalWorkspaceService(
            PersonalWorkspaceItemRepository itemRepository,
            List<PersonalWorkspaceResourceResolver> resolverList) {
        this.itemRepository = itemRepository;
        this.resolvers = new EnumMap<>(PersonalResourceType.class);
        for (PersonalWorkspaceResourceResolver resolver : resolverList) {
            PersonalWorkspaceResourceResolver existing =
                    this.resolvers.put(resolver.resourceType(), resolver);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate personal workspace resolver for " + resolver.resourceType());
            }
        }
    }

    @Transactional(readOnly = true)
    public PersonalWorkspaceOverviewResponse getOverview(
            UUID tenantId, AppUser actor, Integer requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        var page = PageRequest.of(0, limit);

        List<PersonalWorkspaceItemResponse> favorites =
                itemRepository
                        .findByTenant_IdAndUser_IdAndFavoriteAtIsNotNullOrderByFavoriteAtDesc(
                                tenantId, actor.getId(), page)
                        .stream()
                        .map(this::resolveForRead)
                        .flatMap(java.util.Optional::stream)
                        .toList();

        List<PersonalWorkspaceItemResponse> recent =
                itemRepository
                        .findByTenant_IdAndUser_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(
                                tenantId, actor.getId(), page)
                        .stream()
                        .map(this::resolveForRead)
                        .flatMap(java.util.Optional::stream)
                        .toList();

        return new PersonalWorkspaceOverviewResponse(favorites, recent);
    }

    @Transactional
    public PersonalWorkspaceItemResponse favorite(
            UUID tenantId, AppUser actor, PersonalResourceType type, UUID resourceId) {
        PersonalWorkspaceResource resource = requireAccessible(tenantId, actor, type, resourceId);
        PersonalWorkspaceItem item = getOrCreate(tenantId, actor, type, resourceId);
        item.markFavorite(Instant.now());
        PersonalWorkspaceItem saved = itemRepository.save(item);
        return toResponse(resource, saved);
    }

    @Transactional
    public void unfavorite(
            UUID tenantId, AppUser actor, PersonalResourceType type, UUID resourceId) {
        itemRepository
                .findByTenant_IdAndUser_IdAndResourceTypeAndResourceId(
                        tenantId, actor.getId(), type, resourceId)
                .ifPresent(
                        item -> {
                            item.clearFavorite();
                            if (item.getLastViewedAt() == null) {
                                itemRepository.delete(item);
                            } else {
                                itemRepository.save(item);
                            }
                        });
    }

    @Transactional
    public PersonalWorkspaceItemResponse recordRecent(
            UUID tenantId, AppUser actor, PersonalResourceType type, UUID resourceId) {
        PersonalWorkspaceResource resource = requireAccessible(tenantId, actor, type, resourceId);
        PersonalWorkspaceItem item = getOrCreate(tenantId, actor, type, resourceId);
        item.markViewed(Instant.now());
        PersonalWorkspaceItem saved = itemRepository.save(item);
        return toResponse(resource, saved);
    }

    private PersonalWorkspaceItem getOrCreate(
            UUID tenantId, AppUser actor, PersonalResourceType type, UUID resourceId) {
        return itemRepository
                .findByTenant_IdAndUser_IdAndResourceTypeAndResourceId(
                        tenantId, actor.getId(), type, resourceId)
                .orElseGet(
                        () ->
                                new PersonalWorkspaceItem(
                                        actor.getTenant(), actor, type, resourceId));
    }

    private PersonalWorkspaceResource requireAccessible(
            UUID tenantId, AppUser actor, PersonalResourceType type, UUID resourceId) {
        return resolver(type)
                .resolve(tenantId, actor.getId(), resourceId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Resource not found or is not accessible"));
    }

    private java.util.Optional<PersonalWorkspaceItemResponse> resolveForRead(
            PersonalWorkspaceItem item) {
        return resolver(item.getResourceType())
                .resolve(
                        item.getTenant().getId(),
                        item.getUser().getId(),
                        item.getResourceId())
                .map(resource -> toResponse(resource, item));
    }

    private PersonalWorkspaceItemResponse toResponse(
            PersonalWorkspaceResource resource, PersonalWorkspaceItem item) {
        return new PersonalWorkspaceItemResponse(
                resource.type(),
                resource.resourceId(),
                resource.parentId(),
                resource.title(),
                resource.subtitle(),
                item.getFavoriteAt(),
                item.getLastViewedAt());
    }

    private PersonalWorkspaceResourceResolver resolver(PersonalResourceType type) {
        PersonalWorkspaceResourceResolver resolver = resolvers.get(type);
        if (resolver == null) {
            throw new IllegalArgumentException("Unsupported personal resource type: " + type);
        }
        return resolver;
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    }
}
