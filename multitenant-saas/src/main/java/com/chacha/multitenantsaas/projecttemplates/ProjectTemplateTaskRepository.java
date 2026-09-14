package com.chacha.multitenantsaas.projecttemplates;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectTemplateTaskRepository extends JpaRepository<ProjectTemplateTask, UUID> {

    List<ProjectTemplateTask> findByTenantIdAndTemplateIdOrderByPositionIndexAsc(
            UUID tenantId, UUID templateId);

    List<ProjectTemplateTask> findByTenantIdAndTemplateIdInOrderByTemplateIdAscPositionIndexAsc(
            UUID tenantId, Collection<UUID> templateIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "delete from ProjectTemplateTask task "
                    + "where task.tenantId = :tenantId and task.templateId = :templateId")
    int deleteSnapshots(@Param("tenantId") UUID tenantId, @Param("templateId") UUID templateId);
}
