package com.chacha.multitenantsaas.recurringwork;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringTaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RESUME_ADVANCES = 10000;

    private final RecurringTaskDefinitionRepository definitionRepository;
    private final RecurringTaskOccurrenceRepository occurrenceRepository;
    private final CurrentActorService currentActorService;

    public RecurringTaskService(
            RecurringTaskDefinitionRepository definitionRepository,
            RecurringTaskOccurrenceRepository occurrenceRepository,
            CurrentActorService currentActorService) {
        this.definitionRepository = definitionRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.currentActorService = currentActorService;
    }

    @Transactional
    public RecurringTaskDtos.Response create(
            UUID tenantId,
            UUID projectId,
            RecurringTaskDtos.CreateRequest request,
            Jwt jwt) {
        validateSchedule(
                request.zoneId(), request.nextOccurrenceAt(), request.endAt(), request.maxOccurrences(), 0);
        UUID creatorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        RecurringTaskDefinition definition =
                new RecurringTaskDefinition(
                        tenantId,
                        projectId,
                        creatorUserId,
                        request.assigneeUserId(),
                        request.title().trim(),
                        normalizeDescription(request.description()),
                        request.priority(),
                        request.cadence(),
                        request.intervalCount(),
                        request.zoneId().trim(),
                        request.nextOccurrenceAt(),
                        request.dueOffsetMinutes(),
                        request.endAt(),
                        request.maxOccurrences());
        return map(definitionRepository.save(definition));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecurringTaskDtos.Response> list(
            UUID tenantId, UUID projectId, Pageable pageable) {
        Page<RecurringTaskDefinition> page =
                definitionRepository.findByTenantIdAndProjectIdOrderByCreatedAtDesc(
                        tenantId, projectId, bounded(pageable));
        return new PageResponse<>(
                page.getContent().stream().map(this::map).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    @Transactional(readOnly = true)
    public RecurringTaskDtos.Response get(UUID tenantId, UUID projectId, UUID definitionId) {
        return map(requireScoped(tenantId, projectId, definitionId));
    }

    @Transactional
    public RecurringTaskDtos.Response update(
            UUID tenantId,
            UUID projectId,
            UUID definitionId,
            RecurringTaskDtos.UpdateRequest request) {
        RecurringTaskDefinition definition = requireScoped(tenantId, projectId, definitionId);
        validateSchedule(
                request.zoneId(),
                request.nextOccurrenceAt(),
                request.endAt(),
                request.maxOccurrences(),
                definition.getGeneratedCount());
        definition.updateDefinition(
                request.assigneeUserId(),
                request.title().trim(),
                normalizeDescription(request.description()),
                request.priority(),
                request.cadence(),
                request.intervalCount(),
                request.zoneId().trim(),
                request.nextOccurrenceAt(),
                request.dueOffsetMinutes(),
                request.endAt(),
                request.maxOccurrences());
        return map(definitionRepository.save(definition));
    }

    @Transactional
    public RecurringTaskDtos.Response pause(UUID tenantId, UUID projectId, UUID definitionId) {
        RecurringTaskDefinition definition = requireScoped(tenantId, projectId, definitionId);
        if (definition.getStatus() == RecurrenceStatus.ENDED) {
            throw new IllegalArgumentException("Ended recurrence definitions cannot be paused");
        }
        definition.pause(null);
        return map(definitionRepository.save(definition));
    }

    @Transactional
    public RecurringTaskDtos.Response resume(UUID tenantId, UUID projectId, UUID definitionId) {
        RecurringTaskDefinition definition = requireScoped(tenantId, projectId, definitionId);
        if (definition.getStatus() == RecurrenceStatus.ENDED) {
            throw new IllegalArgumentException("Ended recurrence definitions must be edited before resuming");
        }
        Instant occurrence = definition.getNextOccurrenceAt();
        Instant now = Instant.now();
        int advances = 0;
        while (occurrence.isBefore(now)) {
            occurrence = advance(definition, occurrence);
            advances++;
            if (advances > MAX_RESUME_ADVANCES) {
                throw new IllegalStateException("Recurrence is too far behind to resume safely");
            }
        }
        if (definition.getEndAt() != null && occurrence.isAfter(definition.getEndAt())) {
            definition.end();
        } else {
            definition.resumeAt(occurrence);
        }
        return map(definitionRepository.save(definition));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecurringTaskDtos.OccurrenceResponse> occurrences(
            UUID tenantId, UUID projectId, UUID definitionId, Pageable pageable) {
        requireScoped(tenantId, projectId, definitionId);
        Page<RecurringTaskOccurrence> page =
                occurrenceRepository.findByTenantIdAndProjectIdAndDefinitionIdOrderByScheduledForDesc(
                        tenantId, projectId, definitionId, bounded(pageable));
        return new PageResponse<>(
                page.getContent().stream()
                        .map(
                                item ->
                                        new RecurringTaskDtos.OccurrenceResponse(
                                                item.getId(),
                                                item.getScheduledFor(),
                                                item.getTaskId(),
                                                item.getCreatedAt()))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    static Instant advance(RecurringTaskDefinition definition, Instant occurrence) {
        ZoneId zone = ZoneId.of(definition.getZoneId());
        ZonedDateTime current = occurrence.atZone(zone);
        ZonedDateTime next =
                switch (definition.getCadence()) {
                    case DAILY -> current.plusDays(definition.getIntervalCount());
                    case WEEKLY -> current.plusWeeks(definition.getIntervalCount());
                    case MONTHLY -> current.plusMonths(definition.getIntervalCount());
                };
        return next.toInstant();
    }

    private void validateSchedule(
            String zoneId,
            Instant nextOccurrenceAt,
            Instant endAt,
            Integer maxOccurrences,
            int generatedCount) {
        ZoneId.of(zoneId.trim());
        if (endAt != null && endAt.isBefore(nextOccurrenceAt)) {
            throw new IllegalArgumentException("Recurrence end must not precede the next occurrence");
        }
        if (maxOccurrences != null && maxOccurrences <= generatedCount) {
            throw new IllegalArgumentException(
                    "Maximum occurrences must exceed the number already generated");
        }
    }

    private RecurringTaskDefinition requireScoped(
            UUID tenantId, UUID projectId, UUID definitionId) {
        return definitionRepository
                .findByTenantIdAndProjectIdAndId(tenantId, projectId, definitionId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Recurring task definition not found: " + definitionId));
    }

    private Pageable bounded(Pageable pageable) {
        return PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                pageable.getSort());
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private RecurringTaskDtos.Response map(RecurringTaskDefinition definition) {
        return new RecurringTaskDtos.Response(
                definition.getId(),
                definition.getTenantId(),
                definition.getProjectId(),
                definition.getCreatedByUserId(),
                definition.getAssigneeUserId(),
                definition.getTitle(),
                definition.getDescription(),
                definition.getPriority(),
                definition.getCadence(),
                definition.getIntervalCount(),
                definition.getZoneId(),
                definition.getNextOccurrenceAt(),
                definition.getDueOffsetMinutes(),
                definition.getEndAt(),
                definition.getMaxOccurrences(),
                definition.getGeneratedCount(),
                definition.getStatus(),
                definition.getLastError(),
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }
}
