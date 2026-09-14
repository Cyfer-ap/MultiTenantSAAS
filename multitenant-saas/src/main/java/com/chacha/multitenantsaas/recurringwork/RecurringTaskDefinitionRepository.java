package com.chacha.multitenantsaas.recurringwork;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringTaskDefinitionRepository
        extends JpaRepository<RecurringTaskDefinition, UUID> {

    Page<RecurringTaskDefinition> findByTenantIdAndProjectIdOrderByCreatedAtDesc(
            UUID tenantId, UUID projectId, Pageable pageable);

    Optional<RecurringTaskDefinition> findByTenantIdAndProjectIdAndId(
            UUID tenantId, UUID projectId, UUID id);

    @Query(
            """
            SELECT definition.id
            FROM RecurringTaskDefinition definition
            WHERE definition.status = :status
              AND definition.nextOccurrenceAt <= :now
            ORDER BY definition.nextOccurrenceAt ASC
            """)
    Page<UUID> findDueDefinitionIds(
            @Param("status") RecurrenceStatus status, @Param("now") Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT definition FROM RecurringTaskDefinition definition WHERE definition.id = :id")
    Optional<RecurringTaskDefinition> findByIdForUpdate(@Param("id") UUID id);
}
