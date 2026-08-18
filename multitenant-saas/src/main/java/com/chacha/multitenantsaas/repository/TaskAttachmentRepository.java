package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TaskAttachment;
import com.chacha.multitenantsaas.entity.TaskAttachmentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {

    Optional<TaskAttachment> findByTenant_IdAndProject_IdAndTask_IdAndId(
            UUID tenantId, UUID projectId, UUID taskId, UUID attachmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select attachment from TaskAttachment attachment
            where attachment.tenant.id = :tenantId
              and attachment.project.id = :projectId
              and attachment.task.id = :taskId
              and attachment.id = :attachmentId
            """)
    Optional<TaskAttachment> findScopedForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("taskId") UUID taskId,
            @Param("attachmentId") UUID attachmentId);

    List<TaskAttachment> findByTenant_IdAndProject_IdAndTask_IdAndComment_IdAndStatusNot(
            UUID tenantId,
            UUID projectId,
            UUID taskId,
            UUID commentId,
            TaskAttachmentStatus excludedStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TaskAttachment> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            TaskAttachmentStatus status, Instant createdBefore);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TaskAttachment> findTop100ByStatusAndStorageDeletedAtIsNullOrderByDeletedAtAsc(
            TaskAttachmentStatus status);

    @Query(
            """
            select attachment from TaskAttachment attachment
            left join attachment.comment comment
            where attachment.tenant.id = :tenantId
              and attachment.project.id = :projectId
              and attachment.task.id = :taskId
              and attachment.status = :status
              and (comment.id is null or comment.deleted = false)
            """)
    Page<TaskAttachment> findVisibleTaskAttachments(
            @Param("tenantId") UUID tenantId,
            @Param("projectId") UUID projectId,
            @Param("taskId") UUID taskId,
            @Param("status") TaskAttachmentStatus status,
            Pageable pageable);
}
