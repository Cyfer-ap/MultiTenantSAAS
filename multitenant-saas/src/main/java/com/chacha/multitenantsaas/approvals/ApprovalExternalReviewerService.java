package com.chacha.multitenantsaas.approvals;

import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalExternalReviewerService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalRequestStageRepository stageRepository;
    private final ApprovalRequestStageExternalReviewerRepository externalReviewerRepository;
    private final ApprovalExternalGrantPort externalGrantPort;
    private final CurrentActorService currentActorService;

    public ApprovalExternalReviewerService(
            ApprovalRequestRepository requestRepository,
            ApprovalRequestStageRepository stageRepository,
            ApprovalRequestStageExternalReviewerRepository externalReviewerRepository,
            ApprovalExternalGrantPort externalGrantPort,
            CurrentActorService currentActorService) {
        this.requestRepository = requestRepository;
        this.stageRepository = stageRepository;
        this.externalReviewerRepository = externalReviewerRepository;
        this.externalGrantPort = externalGrantPort;
        this.currentActorService = currentActorService;
    }

    @Transactional
    public ApprovalDtos.ExternalReviewerResponse assign(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            UUID grantId,
            Jwt jwt) {
        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        ApprovalRequest request = requirePendingRequestForUpdate(tenantId, projectId, requestId);
        ApprovalRequestStage stage = requireCurrentPendingStage(request);

        ApprovalExternalGrantSnapshot grant =
                externalGrantPort.requireApprovalReviewGrant(tenantId, projectId, grantId);

        return externalReviewerRepository
                .findByTenantIdAndProjectIdAndRequestIdAndRequestStageIdAndExternalAccessGrantId(
                        tenantId, projectId, requestId, stage.getId(), grantId)
                .map(this::map)
                .orElseGet(
                        () ->
                                map(
                                        externalReviewerRepository.saveAndFlush(
                                                new ApprovalRequestStageExternalReviewer(
                                                        tenantId,
                                                        projectId,
                                                        requestId,
                                                        stage.getId(),
                                                        grant,
                                                        actorUserId))));
    }

    @Transactional
    public void revoke(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            UUID grantId) {
        ApprovalRequest request = requirePendingRequestForUpdate(tenantId, projectId, requestId);
        ApprovalRequestStage stage = requireCurrentPendingStage(request);
        ApprovalRequestStageExternalReviewer assignment =
                externalReviewerRepository
                        .findByTenantIdAndProjectIdAndRequestIdAndRequestStageIdAndExternalAccessGrantId(
                                tenantId, projectId, requestId, stage.getId(), grantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "External approval assignment not found"));
        externalReviewerRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<ApprovalDtos.ExternalReviewerResponse> list(
            UUID tenantId, UUID projectId, UUID requestId) {
        ApprovalRequest request =
                requestRepository
                        .findByTenantIdAndProjectIdAndId(tenantId, projectId, requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Approval request not found: " + requestId));
        ApprovalRequestStage stage = requireCurrentStage(request);
        return externalReviewerRepository
                .findByTenantIdAndProjectIdAndRequestIdAndRequestStageId(
                        tenantId, projectId, requestId, stage.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    private ApprovalRequest requirePendingRequestForUpdate(
            UUID tenantId, UUID projectId, UUID requestId) {
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
        return request;
    }

    private ApprovalRequestStage requireCurrentPendingStage(ApprovalRequest request) {
        ApprovalRequestStage stage = requireCurrentStage(request);
        if (stage.getStatus() != ApprovalStageStatus.PENDING) {
            throw new IllegalArgumentException("Approval stage is no longer pending");
        }
        return stage;
    }

    private ApprovalRequestStage requireCurrentStage(ApprovalRequest request) {
        return stageRepository
                .findByTenantIdAndProjectIdAndRequestIdAndPositionIndex(
                        request.getTenantId(),
                        request.getProjectId(),
                        request.getId(),
                        request.getCurrentStageIndex())
                .orElseThrow(
                        () -> new IllegalStateException("Current approval stage is missing"));
    }

    private ApprovalDtos.ExternalReviewerResponse map(
            ApprovalRequestStageExternalReviewer assignment) {
        return new ApprovalDtos.ExternalReviewerResponse(
                assignment.getId(),
                assignment.getRequestId(),
                assignment.getRequestStageId(),
                assignment.getExternalAccessGrantId(),
                assignment.getGuestNameSnapshot(),
                assignment.getGuestEmailSnapshot(),
                assignment.getAssignedByUserId(),
                assignment.getCreatedAt());
    }
}
