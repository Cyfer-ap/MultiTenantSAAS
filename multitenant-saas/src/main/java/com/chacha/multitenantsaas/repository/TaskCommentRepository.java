package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TaskComment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    Page<TaskComment> findByTenant_IdAndProject_IdAndTask_IdAndParentCommentIsNull(
            UUID tenantId, UUID projectId, UUID taskId, Pageable pageable);

    Page<TaskComment> findByTenant_IdAndProject_IdAndTask_IdAndParentComment_Id(
            UUID tenantId, UUID projectId, UUID taskId, UUID parentCommentId, Pageable pageable);

    Optional<TaskComment> findByTenant_IdAndProject_IdAndTask_IdAndId(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TaskComment> findForUpdateByTenant_IdAndProject_IdAndTask_IdAndId(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId);

    List<TaskComment>
            findTop5ByTenant_IdAndProject_IdAndTask_IdAndParentCommentIsNullAndDeletedFalseAndPinnedAtIsNotNullOrderByPinnedAtDesc(
                    UUID tenantId, UUID projectId, UUID taskId);

    long countByTenant_IdAndProject_IdAndTask_IdAndParentCommentIsNullAndDeletedFalseAndPinnedAtIsNotNull(
            UUID tenantId, UUID projectId, UUID taskId);
}
