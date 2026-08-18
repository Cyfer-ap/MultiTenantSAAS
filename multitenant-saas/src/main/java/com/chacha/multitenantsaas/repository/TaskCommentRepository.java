package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TaskComment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    Page<TaskComment> findByTenant_IdAndProject_IdAndTask_Id(
            UUID tenantId, UUID projectId, UUID taskId, Pageable pageable);

    Optional<TaskComment> findByTenant_IdAndProject_IdAndTask_IdAndId(
            UUID tenantId, UUID projectId, UUID taskId, UUID commentId);
}
