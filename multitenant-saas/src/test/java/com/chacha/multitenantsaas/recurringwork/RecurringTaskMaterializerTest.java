package com.chacha.multitenantsaas.recurringwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationResult;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecurringTaskMaterializerTest {

    @Mock private RecurringTaskDefinitionRepository definitionRepository;
    @Mock private RecurringTaskOccurrenceRepository occurrenceRepository;
    @Mock private TaskCreationPort taskCreationPort;

    @Test
    void materializesDueOccurrenceOnceAndEndsAtMaxCount() {
        Instant scheduled = Instant.parse("2026-09-14T10:00:00Z");
        UUID definitionId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        RecurringTaskDefinition definition =
                new RecurringTaskDefinition(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        "Daily task",
                        null,
                        ProjectTaskPriority.MEDIUM,
                        RecurrenceCadence.DAILY,
                        1,
                        "UTC",
                        scheduled,
                        30L,
                        null,
                        1);
        when(definitionRepository.findByIdForUpdate(definitionId))
                .thenReturn(Optional.of(definition));
        when(taskCreationPort.createTask(any()))
                .thenReturn(new TaskCreationResult(taskId, scheduled));

        RecurringTaskMaterializer materializer =
                new RecurringTaskMaterializer(
                        definitionRepository, occurrenceRepository, taskCreationPort);

        int created = materializer.materializeDueDefinition(definitionId, scheduled);

        assertThat(created).isEqualTo(1);
        assertThat(definition.getGeneratedCount()).isEqualTo(1);
        assertThat(definition.getStatus()).isEqualTo(RecurrenceStatus.ENDED);
        verify(taskCreationPort).createTask(any());
        verify(occurrenceRepository).save(any(RecurringTaskOccurrence.class));
        verify(definitionRepository).save(definition);
    }

    @Test
    void doesNotMaterializeFutureDefinition() {
        Instant cutoff = Instant.parse("2026-09-14T10:00:00Z");
        UUID definitionId = UUID.randomUUID();
        RecurringTaskDefinition definition =
                new RecurringTaskDefinition(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        "Future task",
                        null,
                        ProjectTaskPriority.LOW,
                        RecurrenceCadence.WEEKLY,
                        1,
                        "UTC",
                        cutoff.plusSeconds(3600),
                        null,
                        null,
                        null);
        when(definitionRepository.findByIdForUpdate(definitionId))
                .thenReturn(Optional.of(definition));

        RecurringTaskMaterializer materializer =
                new RecurringTaskMaterializer(
                        definitionRepository, occurrenceRepository, taskCreationPort);

        assertThat(materializer.materializeDueDefinition(definitionId, cutoff)).isZero();
        verify(taskCreationPort, never()).createTask(any());
        verify(occurrenceRepository, never()).save(any());
    }
}
