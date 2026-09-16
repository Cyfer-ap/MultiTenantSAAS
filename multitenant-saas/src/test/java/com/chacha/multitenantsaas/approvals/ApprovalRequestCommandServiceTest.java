package com.chacha.multitenantsaas.approvals;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestCommandServiceTest {

    @Mock private ApprovalRequestRepository requestRepository;
    @Mock private ApprovalRequestStageRepository requestStageRepository;
    @Mock private ApprovalRequestStageReviewerRepository requestReviewerRepository;
    @Mock private ApprovalDefinitionSnapshotService snapshotService;
    @Mock private ApprovalReviewerEligibilityPort reviewerEligibilityPort;
    @Mock private CurrentActorService currentActorService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Jwt jwt;
    @Mock private AppUser actor;

    private ApprovalRequestCommandService service;

    @BeforeEach
    void setUp() {
        service =
                new ApprovalRequestCommandService(
                        requestRepository,
                        requestStageRepository,
                        requestReviewerRepository,
                        snapshotService,
                        reviewerEligibilityPort,
                        currentActorService,
                        eventPublisher);
    }

    @Test
    void rejectsConfiguredReviewerWhoIsNoLongerEligibleInProject() {
        Fixture fixture = fixture(false, false);
        stubCurrentActor(fixture.reviewerId());
        stubPendingDecision(fixture);
        when(reviewerEligibilityPort.isEligible(
                        fixture.tenantId(), fixture.projectId(), fixture.reviewerId()))
                .thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.decide(
                                        fixture.tenantId(),
                                        fixture.projectId(),
                                        fixture.requestId(),
                                        new ApprovalDtos.DecisionRequest(
                                                ApprovalDecisionOutcome.APPROVE, null),
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reviewer is no longer an active project member");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void rejectsRequesterSelfApprovalWhenStagePolicyDisallowsIt() {
        Fixture fixture = fixture(false, true);
        stubCurrentActor(fixture.reviewerId());
        stubPendingDecision(fixture);
        when(reviewerEligibilityPort.isEligible(
                        fixture.tenantId(), fixture.projectId(), fixture.reviewerId()))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.decide(
                                        fixture.tenantId(),
                                        fixture.projectId(),
                                        fixture.requestId(),
                                        new ApprovalDtos.DecisionRequest(
                                                ApprovalDecisionOutcome.APPROVE, "self review"),
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Requester self-approval is disabled for this stage");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void rejectsReplayedDecisionAfterRequestIsResolved() {
        Fixture fixture = fixture(true, false);
        fixture.request().approve();
        stubCurrentActor(fixture.reviewerId());
        when(requestRepository.findForDecision(
                        fixture.tenantId(), fixture.projectId(), fixture.requestId()))
                .thenReturn(Optional.of(fixture.request()));

        assertThatThrownBy(
                        () ->
                                service.decide(
                                        fixture.tenantId(),
                                        fixture.projectId(),
                                        fixture.requestId(),
                                        new ApprovalDtos.DecisionRequest(
                                                ApprovalDecisionOutcome.REJECT, null),
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Approval request is already resolved");

        verifyNoInteractions(reviewerEligibilityPort, eventPublisher);
    }

    private void stubCurrentActor(UUID reviewerId) {
        when(currentActorService.getRequiredActiveActor(eq(tenantId()), any(Jwt.class)))
                .thenReturn(actor);
        when(actor.getId()).thenReturn(reviewerId);
    }

    private void stubPendingDecision(Fixture fixture) {
        when(requestRepository.findForDecision(
                        fixture.tenantId(), fixture.projectId(), fixture.requestId()))
                .thenReturn(Optional.of(fixture.request()));
        when(requestStageRepository.findByTenantIdAndProjectIdAndRequestIdAndPositionIndex(
                        fixture.tenantId(), fixture.projectId(), fixture.requestId(), 0))
                .thenReturn(Optional.of(fixture.stage()));
        when(requestReviewerRepository
                        .existsByTenantIdAndProjectIdAndRequestIdAndRequestStageIdAndReviewerUserId(
                                fixture.tenantId(),
                                fixture.projectId(),
                                fixture.requestId(),
                                fixture.stageId(),
                                fixture.reviewerId()))
                .thenReturn(true);
    }

    private Fixture fixture(boolean allowRequesterApproval, boolean reviewerIsRequester) {
        UUID tenantId = tenantId();
        UUID projectId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        UUID requesterId = reviewerIsRequester ? reviewerId : UUID.randomUUID();
        ApprovalCheckpointCommand command =
                new ApprovalCheckpointCommand(
                        tenantId,
                        projectId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        3,
                        UUID.randomUUID(),
                        "approval_1",
                        UUID.randomUUID(),
                        requesterId,
                        "approved_action",
                        "rejected_action");
        ApprovalRequest request = new ApprovalRequest(command, 2);
        ReflectionTestUtils.setField(request, "id", requestId);
        ApprovalRequestStage stage =
                new ApprovalRequestStage(
                        tenantId,
                        projectId,
                        requestId,
                        "manager_review",
                        "Manager review",
                        0,
                        allowRequesterApproval,
                        ApprovalStageStatus.PENDING);
        ReflectionTestUtils.setField(stage, "id", stageId);
        return new Fixture(
                tenantId, projectId, requestId, stageId, reviewerId, request, stage);
    }

    private UUID tenantId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    private record Fixture(
            UUID tenantId,
            UUID projectId,
            UUID requestId,
            UUID stageId,
            UUID reviewerId,
            ApprovalRequest request,
            ApprovalRequestStage stage) {}
}
