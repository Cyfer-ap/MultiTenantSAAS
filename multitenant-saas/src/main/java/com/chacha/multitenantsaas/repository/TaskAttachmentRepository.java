package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TaskAttachment;
import com.chacha.multitenantsaas.entity.TaskAttachmentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {

    Optional<TaskAttachment> findByTenant_IdAndProject_IdAndTask_IdAndId(
            UUID tenantId, UUID projectId, UUID taskId, UUID attachmentId);

    @Query(
            """
            select attachment from TaskAttachment attachment
            where attachment.tenant.id = :tenantId
              and attachment.project.id = :projectId
              and attachment.task.id = :taskId
              and attachment.status = :status
              and (attachment.comment is null or attachment.comment.deleted = false)
            """)
    Page<TaskAttachment> findVisibleTaskAttachments(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("taskId") UUID taskId,
            @Param("status") TaskAttachmentStatus status,
            Pageable pageable);
}
