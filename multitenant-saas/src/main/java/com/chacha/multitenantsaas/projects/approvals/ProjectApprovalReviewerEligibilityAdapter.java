package com.chacha.multitenantsaas.projects.approvals;

import com.chacha.multitenantsaas.approvals.ApprovalReviewerEligibilityPort;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectApprovalReviewerEligibilityAdapter implements ApprovalReviewerEligibilityPort {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectApprovalReviewerEligibilityAdapter(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEligible(UUID tenantId, UUID projectId, UUID userId) {
        return projectMemberRepository
                .findByProject_Tenant_IdAndProject_IdAndUser_Id(tenantId, projectId, userId)
                .map(member -> member.getUser().getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }
}
