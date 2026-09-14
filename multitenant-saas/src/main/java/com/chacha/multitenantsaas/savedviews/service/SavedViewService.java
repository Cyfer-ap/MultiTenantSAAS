package com.chacha.multitenantsaas.savedviews.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.savedviews.dto.CreateSavedViewRequest;
import com.chacha.multitenantsaas.savedviews.dto.SavedViewResponse;
import com.chacha.multitenantsaas.savedviews.dto.UpdateSavedViewRequest;
import com.chacha.multitenantsaas.savedviews.entity.SavedView;
import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
import com.chacha.multitenantsaas.savedviews.repository.SavedViewRepository;
import com.chacha.multitenantsaas.savedviews.spi.SavedViewContextValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedViewService {

    private static final int MAX_VIEWS_PER_SCOPE = 100;
    private static final int MAX_DEFINITION_ENTRIES = 16;
    private static final int MAX_SEARCH_LENGTH = 100;
    private static final int MAX_DEFINITION_JSON_LENGTH = 4000;

    private static final Set<String> MY_WORK_KEYS =
            Set.of("search", "attention", "status", "priority");
    private static final Set<String> PROJECT_TASK_KEYS =
            Set.of(
                    "search",
                    "status",
                    "priority",
                    "assigneeUserId",
                    "sortBy",
                    "sortDir",
                    "layout",
                    "showCancelled");
    private static final Set<String> ATTENTION_VALUES =
            Set.of("OVERDUE", "BLOCKED", "DUE_SOON", "IN_PROGRESS", "ASSIGNED");
    private static final Set<String> OPEN_STATUS_VALUES = Set.of("TODO", "IN_PROGRESS", "BLOCKED");
    private static final Set<String> TASK_STATUS_VALUES =
            Set.of("TODO", "IN_PROGRESS", "BLOCKED", "COMPLETED", "CANCELLED");
    private static final Set<String> PRIORITY_VALUES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
    private static final Set<String> TASK_SORT_VALUES =
            Set.of("createdAt", "updatedAt", "title", "status", "priority", "dueAt");

    private final SavedViewRepository repository;
    private final ObjectMapper objectMapper;
    private final Map<SavedViewTarget, SavedViewContextValidator> contextValidators;

    public SavedViewService(
            SavedViewRepository repository,
            ObjectMapper objectMapper,
            List<SavedViewContextValidator> validators) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.contextValidators = new EnumMap<>(SavedViewTarget.class);
        for (SavedViewContextValidator validator : validators) {
            SavedViewContextValidator existing =
                    contextValidators.put(validator.target(), validator);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate saved-view context validator for " + validator.target());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<SavedViewResponse> list(
            UUID tenantId,
            AppUser actor,
            SavedViewTarget target,
            UUID contextId) {
        validateContext(tenantId, actor, target, contextId);
        return repository
                .findByTenant_IdAndUser_IdAndTargetAndContextIdOrderByNameAsc(
                        tenantId,
                        actor.getId(),
                        target,
                        contextId,
                        PageRequest.of(0, MAX_VIEWS_PER_SCOPE))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SavedViewResponse create(
            UUID tenantId, AppUser actor, CreateSavedViewRequest request) {
        validateContext(tenantId, actor, request.target(), request.contextId());
        long existingCount =
                repository.countByTenant_IdAndUser_IdAndTargetAndContextId(
                        tenantId, actor.getId(), request.target(), request.contextId());
        if (existingCount >= MAX_VIEWS_PER_SCOPE) {
            throw new IllegalArgumentException("Saved view limit reached for this workspace surface");
        }

        Map<String, String> definition =
                normalizeDefinition(request.target(), request.definition());
        SavedView saved =
                repository.save(
                        new SavedView(
                                actor.getTenant(),
                                actor,
                                normalizeName(request.name()),
                                request.target(),
                                request.contextId(),
                                serialize(definition)));
        return toResponse(saved);
    }

    @Transactional
    public SavedViewResponse update(
            UUID tenantId, AppUser actor, UUID viewId, UpdateSavedViewRequest request) {
        SavedView savedView = requireOwned(tenantId, actor.getId(), viewId);
        validateContext(tenantId, actor, savedView.getTarget(), savedView.getContextId());
        Map<String, String> definition =
                normalizeDefinition(savedView.getTarget(), request.definition());
        savedView.update(normalizeName(request.name()), serialize(definition));
        return toResponse(repository.save(savedView));
    }

    @Transactional
    public void delete(UUID tenantId, AppUser actor, UUID viewId) {
        repository.delete(requireOwned(tenantId, actor.getId(), viewId));
    }

    private SavedView requireOwned(UUID tenantId, UUID userId, UUID viewId) {
        return repository
                .findByTenant_IdAndUser_IdAndId(tenantId, userId, viewId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved view not found"));
    }

    private void validateContext(
            UUID tenantId, AppUser actor, SavedViewTarget target, UUID contextId) {
        if (target == SavedViewTarget.MY_WORK) {
            if (contextId != null) {
                throw new IllegalArgumentException("My Work saved views cannot have a context id");
            }
            return;
        }

        if (contextId == null) {
            throw new IllegalArgumentException("Project task saved views require a project context");
        }
        SavedViewContextValidator validator = contextValidators.get(target);
        if (validator == null || !validator.canUse(tenantId, actor.getId(), contextId)) {
            throw new ResourceNotFoundException("Saved view context not found or is not accessible");
        }
    }

    private Map<String, String> normalizeDefinition(
            SavedViewTarget target, Map<String, String> input) {
        if (input.size() > MAX_DEFINITION_ENTRIES) {
            throw new IllegalArgumentException("Saved view definition has too many filters");
        }

        Map<String, String> normalized = new TreeMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            validateEntry(target, key, value);
            normalized.put(key, value);
        }
        return new LinkedHashMap<>(normalized);
    }

    private void validateEntry(SavedViewTarget target, String key, String value) {
        Set<String> allowedKeys =
                target == SavedViewTarget.MY_WORK ? MY_WORK_KEYS : PROJECT_TASK_KEYS;
        if (!allowedKeys.contains(key)) {
            throw new IllegalArgumentException("Unsupported saved view filter: " + key);
        }
        if ("search".equals(key)) {
            if (value.length() > MAX_SEARCH_LENGTH) {
                throw new IllegalArgumentException("Saved view search is too long");
            }
            return;
        }

        if (target == SavedViewTarget.MY_WORK) {
            switch (key) {
                case "attention" -> requireOneOf(key, value, ATTENTION_VALUES);
                case "status" -> requireOneOf(key, value, OPEN_STATUS_VALUES);
                case "priority" -> requireOneOf(key, value, PRIORITY_VALUES);
                default -> throw new IllegalArgumentException("Unsupported My Work filter: " + key);
            }
            return;
        }

        switch (key) {
            case "status" -> requireOneOf(key, value, TASK_STATUS_VALUES);
            case "priority" -> requireOneOf(key, value, PRIORITY_VALUES);
            case "assigneeUserId" -> parseUuid(key, value);
            case "sortBy" -> requireOneOf(key, value, TASK_SORT_VALUES);
            case "sortDir" -> requireOneOf(key, value, Set.of("asc", "desc"));
            case "layout" -> requireOneOf(key, value, Set.of("board", "table"));
            case "showCancelled" -> requireOneOf(key, value, Set.of("true", "false"));
            default -> throw new IllegalArgumentException("Unsupported project task filter: " + key);
        }
    }

    private void requireOneOf(String key, String value, Set<String> allowed) {
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException("Invalid saved view value for " + key);
        }
    }

    private void parseUuid(String key, String value) {
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid saved view value for " + key, exception);
        }
    }

    private String normalizeName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty() || name.length() > 80) {
            throw new IllegalArgumentException("Saved view name must contain 1 to 80 characters");
        }
        return name;
    }

    private String serialize(Map<String, String> definition) {
        try {
            String serialized = objectMapper.writeValueAsString(definition);
            if (serialized.length() > MAX_DEFINITION_JSON_LENGTH) {
                throw new IllegalArgumentException("Saved view definition is too large");
            }
            return serialized;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Saved view definition could not be serialized", exception);
        }
    }

    private Map<String, String> deserialize(String definitionJson) {
        try {
            return objectMapper.readValue(
                    definitionJson, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored saved view definition is invalid", exception);
        }
    }

    private SavedViewResponse toResponse(SavedView savedView) {
        return new SavedViewResponse(
                savedView.getId(),
                savedView.getName(),
                savedView.getTarget(),
                savedView.getContextId(),
                deserialize(savedView.getDefinitionJson()),
                savedView.getCreatedAt(),
                savedView.getUpdatedAt());
    }
}
