package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.TaskActivity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, UUID> {

    Page<TaskActivity> findByTenant_IdAndProject_IdAndTask_Id(
            UUID tenantId, UUID projectId, UUID taskId, Pageable pageable);
}
