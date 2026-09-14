package com.chacha.multitenantsaas.recurringwork;

import com.chacha.multitenantsaas.tasks.creation.TaskCreationCommand;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringTaskMaterializer {

    private static final int DUE_BATCH_SIZE = 50;
    private static final int MAX_CATCH_UP_PER_DEFINITION = 5;

    private final RecurringTaskDefinitionRepository definitionRepository;
    private final RecurringTaskOccurrenceRepository occurrenceRepository;
    private final TaskCreationPort taskCreationPort;

    public RecurringTaskMaterializer(
            RecurringTaskDefinitionRepository definitionRepository,
            RecurringTaskOccurrenceRepository occurrenceRepository,
            TaskCreationPort taskCreationPort) {
        this.definitionRepository = definitionRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.taskCreationPort = taskCreationPort;
    }

    @Transactional(readOnly = true)
    public List<UUID> findDueDefinitionIds(Instant now) {
        return definitionRepository
                .findDueDefinitionIds(
                        RecurrenceStatus.ACTIVE, now, PageRequest.of(0, DUE_BATCH_SIZE))
                .getContent();
    }

    @Transactional
    public int materializeDueDefinition(UUID definitionId, Instant cutoff) {
        RecurringTaskDefinition definition =
                definitionRepository.findByIdForUpdate(definitionId).orElse(null);
        if (definition == null
                || definition.getStatus() != RecurrenceStatus.ACTIVE
                || definition.getNextOccurrenceAt().isAfter(cutoff)) {
            return 0;
        }

        int created = 0;
        while (created < MAX_CATCH_UP_PER_DEFINITION
                && definition.getStatus() == RecurrenceStatus.ACTIVE
                && !definition.getNextOccurrenceAt().isAfter(cutoff)) {
            if (hasReachedEnd(definition, definition.getNextOccurrenceAt())) {
                definition.end();
                break;
            }

            Instant scheduledFor = definition.getNextOccurrenceAt();
            Instant dueAt =
                    definition.getDueOffsetMinutes() == null
                            ? null
                            : scheduledFor.plusSeconds(definition.getDueOffsetMinutes() * 60L);
            TaskCreationResult task =
                    taskCreationPort.createTask(
                            new TaskCreationCommand(
                                    definition.getTenantId(),
                                    definition.getProjectId(),
                                    definition.getCreatedByUserId(),
                                    definition.getAssigneeUserId(),
                                    definition.getTitle(),
                                    definition.getDescription(),
                                    definition.getPriority(),
                                    dueAt,
                                    "Task generated from recurring work"));
            occurrenceRepository.save(
                    new RecurringTaskOccurrence(
                            definition.getTenantId(),
                            definition.getProjectId(),
                            definition.getId(),
                            scheduledFor,
                            task.taskId()));

            Instant next = RecurringTaskService.advance(definition, scheduledFor);
            definition.markMaterialized(next);
            created++;

            if (hasReachedCount(definition)
                    || (definition.getEndAt() != null && next.isAfter(definition.getEndAt()))) {
                definition.end();
            }
        }
        definitionRepository.save(definition);
        return created;
    }

    @Transactional
    public void pauseAfterFailure(UUID definitionId, String errorMessage) {
        definitionRepository
                .findByIdForUpdate(definitionId)
                .ifPresent(
                        definition -> {
                            if (definition.getStatus() == RecurrenceStatus.ACTIVE) {
                                definition.pause(truncate(errorMessage));
                                definitionRepository.save(definition);
                            }
                        });
    }

    private boolean hasReachedEnd(RecurringTaskDefinition definition, Instant scheduledFor) {
        return hasReachedCount(definition)
                || (definition.getEndAt() != null && scheduledFor.isAfter(definition.getEndAt()));
    }

    private boolean hasReachedCount(RecurringTaskDefinition definition) {
        return definition.getMaxOccurrences() != null
                && definition.getGeneratedCount() >= definition.getMaxOccurrences();
    }

    private String truncate(String message) {
        String safe =
                message == null || message.isBlank()
                        ? "Recurring task generation failed"
                        : message;
        return safe.length() <= 500 ? safe : safe.substring(0, 500);
    }
}
