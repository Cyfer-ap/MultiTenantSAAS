package com.chacha.multitenantsaas.recurringwork;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(RecurringTaskWorker.class);

    private final RecurringTaskMaterializer materializer;

    public RecurringTaskWorker(RecurringTaskMaterializer materializer) {
        this.materializer = materializer;
    }

    @Scheduled(
            initialDelayString = "${app.recurring-work.initial-delay-ms:60000}",
            fixedDelayString = "${app.recurring-work.interval-ms:60000}")
    public void processDueDefinitions() {
        Instant cutoff = Instant.now();
        for (UUID definitionId : materializer.findDueDefinitionIds(cutoff)) {
            try {
                materializer.materializeDueDefinition(definitionId, cutoff);
            } catch (RuntimeException exception) {
                log.warn(
                        "Pausing recurring task definition {} after materialization failure",
                        definitionId,
                        exception);
                try {
                    materializer.pauseAfterFailure(definitionId, exception.getMessage());
                } catch (RuntimeException pauseException) {
                    log.error(
                            "Could not pause failed recurring task definition {}",
                            definitionId,
                            pauseException);
                }
            }
        }
    }
}
