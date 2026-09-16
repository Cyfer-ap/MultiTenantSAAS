package com.chacha.multitenantsaas.approvals;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalRequestQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalRequestStageRepository stageRepository;
    private final ApprovalRequestStageReviewerRepository reviewerRepository;
    private final ApprovalReviewerEligibilityPort reviewerEligibilityPort;
    private final CurrentActorService currentActorService;

    public ApprovalRequestQueryService(
            ApprovalRequestRepository requestRepository,
            ApprovalRequestStageRepository stageRepository,
            ApprovalRequestStageReviewerRepository reviewerRepository,
            ApprovalReviewerEligibilityPort reviewerEligibilityPort,
            CurrentActorService currentActorService) {
        this.requestRepository = requestRepository;
        this.stageRepository = stageRepository;
        this.reviewerRepository = reviewerRepository;
        this.reviewerEligibilityPort = reviewerEligibilityPort;
        this.currentActorService = currentActorService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalDtos.RequestSummary> history(
            UUID tenantId, UUID projectId, Pageable pageable) {
        Page<ApprovalRequest> page =
                requestRepository.findByTenantIdAndProjectIdOrderByCreatedAtDesc(
                        tenantId, projectId, bounded(pageable));
        return page(page, page.getContent().stream().map(this::summary).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalDtos.RequestSummary> inbox(
            UUID tenantId, UUID projectId, Pageable pageable, Jwt jwt) {
        UUID actor = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        Pageable bounded = bounded(pageable);
        if (!reviewerEligibilityPort.isEligible(tenantId, projectId, actor)) {
            return new PageResponse<>(
                    List.of(),
                    bounded.getPageNumber(),
                    bounded.getPageSize(),
                    0,
                    0,
                    true,
                    true);
        }
        Page<ApprovalRequestStageReviewer> reviewerPage =
                reviewerRepository.findPendingInbox(tenantId, projectId, actor, bounded);
        List<ApprovalDtos.RequestSummary> content =
                reviewerPage.getContent().stream()
                        .map(
                                reviewer ->
                                        requestRepository
                                                .findByTenantIdAndProjectIdAndId(
                                                        tenantId,
                                                        projectId,
                                                        reviewer.getRequestId())
                                                .orElseThrow())
                        .map(this::summary)
                        .toList();
        return new PageResponse<>(
                content,
                reviewerPage.getNumber(),
                reviewerPage.getSize(),
                reviewerPage.getTotalElements(),
                reviewerPage.getTotalPages(),
                reviewerPage.isFirst(),
                reviewerPage.isLast());
    }

    @Transactional(readOnly = true)
    public ApprovalDtos.RequestResponse get(UUID tenantId, UUID projectId, UUID requestId) {
        ApprovalRequest request =
                requestRepository
                        .findByTenantIdAndProjectIdAndId(tenantId, projectId, requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Approval request not found: " + requestId));
        List<ApprovalDtos.RequestStageResponse> stages =
                stageRepository
                        .findByTenantIdAndProjectIdAndRequestIdOrderByPositionIndexAsc(
                                tenantId, projectId, requestId)
                        .stream()
                        .map(stage -> stageResponse(tenantId, projectId, requestId, stage))
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

    private ApprovalDtos.RequestStageResponse stageResponse(
            UUID tenantId, UUID projectId, UUID requestId, ApprovalRequestStage stage) {
        return new ApprovalDtos.RequestStageResponse(
                stage.getId(),
                stage.getStageKey(),
                stage.getStageName(),
                stage.getPositionIndex(),
                stage.isAllowRequesterApproval(),
                stage.getStatus(),
                reviewerRepository
                        .findByTenantIdAndProjectIdAndRequestIdAndRequestStageIdOrderByReviewerUserIdAsc(
                                tenantId, projectId, requestId, stage.getId())
                        .stream()
                        .map(ApprovalRequestStageReviewer::getReviewerUserId)
                        .toList(),
                stage.getDecidedByUserId(),
                stage.getDecisionComment(),
                stage.getDecidedAt());
    }

    private ApprovalDtos.RequestSummary summary(ApprovalRequest request) {
        String currentStageName =
                stageRepository
                        .findByTenantIdAndProjectIdAndRequestIdAndPositionIndex(
                                request.getTenantId(),
                                request.getProjectId(),
                                request.getId(),
                                request.getCurrentStageIndex())
                        .map(ApprovalRequestStage::getStageName)
                        .orElse(null);
        return new ApprovalDtos.RequestSummary(
                request.getId(),
                request.getDefinitionId(),
                request.getWorkflowId(),
                request.getTaskId(),
                request.getStatus(),
                request.getCurrentStageIndex(),
                currentStageName,
                request.getCreatedAt(),
                request.getCompletedAt());
    }

    private <T> PageResponse<T> page(Page<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private Pageable bounded(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE)));
    }
}
