package com.chacha.multitenantsaas.forms;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID> {

    Page<FormSubmission> findByTenantIdAndProjectIdAndFormIdOrderBySubmittedAtDesc(
            UUID tenantId, UUID projectId, UUID formId, Pageable pageable);
}
