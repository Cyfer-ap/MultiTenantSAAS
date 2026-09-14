package com.chacha.multitenantsaas.recurringwork;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTaskOccurrenceRepository
        extends JpaRepository<RecurringTaskOccurrence, UUID> {

    Page<RecurringTaskOccurrence> findByTenantIdAndProjectIdAndDefinitionIdOrderByScheduledForDesc(
            UUID tenantId, UUID projectId, UUID definitionId, Pageable pageable);
}
