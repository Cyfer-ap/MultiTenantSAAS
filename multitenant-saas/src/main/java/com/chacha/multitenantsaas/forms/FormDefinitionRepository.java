package com.chacha.multitenantsaas.forms;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, UUID> {

    Page<FormDefinition> findByTenantIdAndProjectIdOrderByNameAsc(
            UUID tenantId, UUID projectId, Pageable pageable);

    @Query(
            "select distinct f from FormDefinition f left join fetch f.fields "
                    + "where f.tenantId = :tenantId and f.projectId = :projectId and f.id = :formId")
    Optional<FormDefinition> findDetailed(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("formId") UUID formId);

    boolean existsByTenantIdAndProjectIdAndNormalizedName(
            UUID tenantId, UUID projectId, String normalizedName);

    boolean existsByTenantIdAndProjectIdAndNormalizedNameAndIdNot(
            UUID tenantId, UUID projectId, String normalizedName, UUID id);
}
