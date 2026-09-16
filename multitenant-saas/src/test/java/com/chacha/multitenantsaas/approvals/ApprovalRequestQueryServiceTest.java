package com.chacha.multitenantsaas.approvals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestQueryServiceTest {

    @Mock private ApprovalRequestRepository requestRepository;
    @Mock private ApprovalRequestStageRepository stageRepository;
    @Mock private ApprovalRequestStageReviewerRepository reviewerRepository;
    @Mock private ApprovalReviewerEligibilityPort reviewerEligibilityPort;
    @Mock private CurrentActorService currentActorService;
    @Mock private Jwt jwt;
    @Mock private AppUser actor;

    private ApprovalRequestQueryService service;

    @BeforeEach
    void setUp() {
        service =
                new ApprovalRequestQueryService(
                        requestRepository,
                        stageRepository,
                        reviewerRepository,
                        reviewerEligibilityPort,
                        currentActorService);
    }

    @Test
    void hidesInboxWhenSnapshottedReviewerIsNoLongerEligible() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(currentActorService.getRequiredActiveActor(eq(tenantId), any(Jwt.class)))
                .thenReturn(actor);
        when(actor.getId()).thenReturn(actorId);
        when(reviewerEligibilityPort.isEligible(tenantId, projectId, actorId)).thenReturn(false);

        var page = service.inbox(tenantId, projectId, PageRequest.of(0, 25), jwt);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        verify(reviewerRepository, never()).findPendingInbox(any(), any(), any(), any());
    }
}
