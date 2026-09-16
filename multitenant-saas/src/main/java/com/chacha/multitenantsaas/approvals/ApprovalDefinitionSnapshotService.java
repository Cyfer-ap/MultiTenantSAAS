package com.chacha.multitenantsaas.approvals;

import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalDefinitionSnapshotService {

    record StageSnapshot(
            String key,
            String name,
            int position,
            boolean allowRequesterApproval,
            List<UUID> reviewerUserIds) {}

    record DefinitionSnapshot(
            UUID id, int version, ApprovalStatus status, List<StageSnapshot> stages) {}

    private final ApprovalDefinitionRepository definitionRepository;
    private final ApprovalStageRepository stageRepository;
    private final ApprovalStageReviewerRepository reviewerRepository;
    private final ApprovalReviewerEligibilityPort reviewerEligibilityPort;

    public ApprovalDefinitionSnapshotService(
            ApprovalDefinitionRepository definitionRepository,
            ApprovalStageRepository stageRepository,
            ApprovalStageReviewerRepository reviewerRepository,
            ApprovalReviewerEligibilityPort reviewerEligibilityPort) {
        this.definitionRepository = definitionRepository;
        this.stageRepository = stageRepository;
        this.reviewerRepository = reviewerRepository;
        this.reviewerEligibilityPort = reviewerEligibilityPort;
    }

    @Transactional(readOnly = true)
    DefinitionSnapshot requireActive(UUID tenantId, UUID projectId, UUID definitionId) {
        ApprovalDefinition definition =
                definitionRepository
                        .findByTenantIdAndProjectIdAndId(tenantId, projectId, definitionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Approval definition not found: " + definitionId));
        if (definition.getStatus() != ApprovalStatus.ACTIVE) {
            throw new IllegalArgumentException("Approval definition must be active");
        }
        List<StageSnapshot> stages =
                stageRepository
                        .findByTenantIdAndProjectIdAndDefinitionIdOrderByPositionIndexAsc(
                                tenantId, projectId, definitionId)
                        .stream()
                        .map(
                                stage -> {
                                    List<UUID> reviewers =
                                            reviewerRepository
                                                    .findByTenantIdAndProjectIdAndDefinitionIdAndStageIdOrderByReviewerUserIdAsc(
                                                            tenantId,
                                                            projectId,
                                                            definitionId,
                                                            stage.getId())
                                                    .stream()
                                                    .map(ApprovalStageReviewer::getReviewerUserId)
                                                    .toList();
                                    if (reviewers.isEmpty()) {
                                        throw new IllegalStateException(
                                                "Stored approval stage has no reviewers");
                                    }
                                    for (UUID reviewer : reviewers) {
                                        if (!reviewerEligibilityPort.isEligible(
                                                tenantId, projectId, reviewer)) {
                                            throw new IllegalArgumentException(
                                                    "Approval reviewer is no longer an active project member: "
                                                            + reviewer);
                                        }
                                    }
                                    return new StageSnapshot(
                                            stage.getStageKey(),
                                            stage.getName(),
                                            stage.getPositionIndex(),
                                            stage.isAllowRequesterApproval(),
                                            reviewers);
                                })
                        .toList();
        if (stages.isEmpty()) {
            throw new IllegalStateException("Stored approval definition has no stages");
        }
        return new DefinitionSnapshot(
                definition.getId(),
                definition.getDefinitionVersion(),
                definition.getStatus(),
                stages);
    }
}
