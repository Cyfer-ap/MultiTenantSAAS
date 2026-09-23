package com.chacha.multitenantsaas.approvals;

import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalRequestCommandService implements ApprovalCheckpointPort {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalRequestStageRepository requestStageRepository;
    private final ApprovalRequestStageReviewerRepository requestReviewerRepository;
    private final ApprovalDefinitionSnapshotService snapshotService;
    private final ApprovalReviewerEligibilityPort reviewerEligibilityPort;
    private final CurrentActorService currentActorService;
    private final ApplicationEventPublisher eventPublisher;

    public ApprovalRequestCommandService(
            ApprovalRequestRepository requestRepository,
            ApprovalRequestStageRepository requestStageRepository,
            ApprovalRequestStageReviewerRepository requestReviewerRepository,
            ApprovalDefinitionSnapshotService snapshotService,
            ApprovalReviewerEligibilityPort reviewerEligibilityPort,
            CurrentActorService currentActorService,
            ApplicationEventPublisher eventPublisher) {
        this.requestRepository = requestRepository;
        this.requestStageRepository = requestStageRepository;
        this.requestReviewerRepository = requestReviewerRepository;
        this.snapshotService = snapshotService;
        this.reviewerEligibilityPort = reviewerEligibilityPort;
        this.currentActorService = currentActorService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID openCheckpoint(ApprovalCheckpointCommand command) {
        return requestRepository
                .findByTenantIdAndWorkflowExecutionIdAndWorkflowNodeKey(
                        command.tenantId(),
                        command.workflowExecutionId(),
                        command.workflowNodeKey())
                .map(ApprovalRequest::getId)
                .orElseGet(() -> createCheckpoint(command));
    }

    @Transactional
    public ApprovalDtos.RequestResponse decide(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            ApprovalDtos.DecisionRequest decision,
            Jwt jwt) {
        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        ApprovalRequest request =
                requestRepository
                        .findForDecision(tenantId, projectId, requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Approval request not found: " + requestId));
        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new IllegalArgumentException("Approval request is already resolved");
        }
        ApprovalRequestStage stage =
                requestStageRepository
                        .findByTenantIdAndProjectIdAndRequestIdAndPositionIndex(
                                tenantId, projectId, requestId, request.getCurrentStageIndex())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Current approval stage is missing"));
        if (!requestReviewerRepository
                .existsByTenantIdAndProjectIdAndRequestIdAndRequestStageIdAndReviewerUserId(
                        tenantId, projectId, requestId, stage.getId(), actorUserId)) {
            throw new IllegalArgumentException(
                    "Current user is not a reviewer for this approval stage");
        }
        if (!reviewerEligibilityPort.isEligible(tenantId, projectId, actorUserId)) {
            throw new IllegalArgumentException("Reviewer is no longer an active project member");
        }
        if (!stage.isAllowRequesterApproval() && actorUserId.equals(request.getActorUserId())) {
            throw new IllegalArgumentException(
                    "Requester self-approval is disabled for this stage");
        }

        stage.decide(actorUserId, decision.outcome(), decision.comment());
        requestStageRepository.save(stage);

        if (decision.outcome() == ApprovalDecisionOutcome.REJECT) {
            request.reject();
            requestRepository.saveAndFlush(request);
            publishResolved(request);
        } else {
            int nextIndex = request.getCurrentStageIndex() + 1;
            var nextStage =
                    requestStageRepository.findByTenantIdAndProjectIdAndRequestIdAndPositionIndex(
                            tenantId, projectId, requestId, nextIndex);
            if (nextStage.isPresent()) {
                nextStage.get().activate();
                requestStageRepository.save(nextStage.get());
                request.advance();
                requestRepository.saveAndFlush(request);
            } else {
                request.approve();
                requestRepository.saveAndFlush(request);
                publishResolved(request);
            }
        }
        return map(request);
    }

    private UUID createCheckpoint(ApprovalCheckpointCommand command) {
        ApprovalDefinitionSnapshotService.DefinitionSnapshot snapshot =
                snapshotService.requireActive(
                        command.tenantId(), command.projectId(), command.approvalDefinitionId());
        ApprovalRequest request =
                requestRepository.saveAndFlush(new ApprovalRequest(command, snapshot.version()));
        for (ApprovalDefinitionSnapshotService.StageSnapshot stageSnapshot : snapshot.stages()) {
            ApprovalRequestStage stage =
                    requestStageRepository.saveAndFlush(
                            new ApprovalRequestStage(
                                    command.tenantId(),
                                    command.projectId(),
                                    request.getId(),
                                    stageSnapshot.key(),
                                    stageSnapshot.name(),
                                    stageSnapshot.position(),
                                    stageSnapshot.allowRequesterApproval(),
                                    stageSnapshot.position() == 0
                                            ? ApprovalStageStatus.PENDING
                                            : ApprovalStageStatus.WAITING));
            requestReviewerRepository.saveAll(
                    stageSnapshot.reviewerUserIds().stream()
                            .map(
                                    reviewerId ->
                                            new ApprovalRequestStageReviewer(
                                                    command.tenantId(),
                                                    command.projectId(),
                                                    request.getId(),
                                                    stage.getId(),
                                                    reviewerId))
                            .toList());
        }
        return request.getId();
    }

    private void publishResolved(ApprovalRequest request) {
        eventPublisher.publishEvent(
                new ApprovalResolvedEvent(
                        request.getId(),
                        request.getTenantId(),
                        request.getProjectId(),
                        request.getWorkflowId(),
                        request.getWorkflowVersion(),
                        request.getWorkflowExecutionId(),
                        request.getWorkflowNodeKey(),
                        request.getTaskId(),
                        request.getActorUserId(),
                        request.getStatus(),
                        request.getApprovedNextNodeKey(),
                        request.getRejectedNextNodeKey()));
    }

    private ApprovalDtos.RequestResponse map(ApprovalRequest request) {
        List<ApprovalDtos.RequestStageResponse> stages =
                requestStageRepository
                        .findByTenantIdAndProjectIdAndRequestIdOrderByPositionIndexAsc(
                                request.getTenantId(), request.getProjectId(), request.getId())
                        .stream()
                        .map(
                                stage ->
                                        new ApprovalDtos.RequestStageResponse(
                                                stage.getId(),
                                                stage.getStageKey(),
                                                stage.getStageName(),
                                                stage.getPositionIndex(),
                                                stage.isAllowRequesterApproval(),
                                                stage.getStatus(),
                                                requestReviewerRepository
                                                        .findByTenantIdAndProjectIdAndRequestIdAndRequestStageIdOrderByReviewerUserIdAsc(
                                                                request.getTenantId(),
                                                                request.getProjectId(),
                                                                request.getId(),
                                                                stage.getId())
                                                        .stream()
                                                        .map(
                                                                ApprovalRequestStageReviewer
                                                                        ::getReviewerUserId)
                                                        .toList(),
                                                stage.getDecidedByUserId(),
                                                stage.getDecisionComment(),
                                                stage.getDecidedAt()))
                        .toList();
        return new ApprovalDtos.RequestResponse(
                request.getId(),
                request.getProjectId(),
                request.getDefinitionId(),
                request.getDefinitionVersion(),
                request.getWorkflowId(),
                request.getWorkflowVersion(),
                request.getWorkflowExecutionId(),
                request.getWorkflowNodeKey(),
                request.getTaskId(),
                request.getActorUserId(),
                request.getStatus(),
                request.getCurrentStageIndex(),
                stages,
                request.getCreatedAt(),
                request.getCompletedAt());
    }
}
