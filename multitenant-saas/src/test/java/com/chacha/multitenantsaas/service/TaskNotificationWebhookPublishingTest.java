package com.chacha.multitenantsaas.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.OutboundWebhookCommentPayload;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.TaskComment;
import com.chacha.multitenantsaas.entity.Tenant;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskNotificationWebhookPublishingTest {

    @Mock private NotificationService notificationService;
    @Mock private OutboundWebhookEventService outboundWebhookEventService;
    @Mock private Tenant tenant;
    @Mock private Project project;
    @Mock private ProjectTask task;
    @Mock private TaskComment comment;
    @Mock private TaskComment parent;
    @Mock private AppUser actor;
    @Mock private AppUser author;

    private TaskNotificationService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        service = new TaskNotificationService(notificationService, outboundWebhookEventService);
        tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(tenant.getId()).thenReturn(tenantId);
        when(project.getId()).thenReturn(projectId);
        when(task.getId()).thenReturn(taskId);
        when(task.getTenant()).thenReturn(tenant);
        when(comment.getTenant()).thenReturn(tenant);
        when(comment.getProject()).thenReturn(project);
        when(comment.getTask()).thenReturn(task);
        when(comment.getAuthorUser()).thenReturn(author);
        when(comment.getId()).thenReturn(UUID.randomUUID());
        when(comment.getBody()).thenReturn("Webhook-safe comment");
        when(comment.getCreatedAt()).thenReturn(Instant.parse("2026-09-07T12:00:00Z"));
        when(comment.getUpdatedAt()).thenReturn(Instant.parse("2026-09-07T12:00:00Z"));
        when(author.getId()).thenReturn(authorId);
    }

    @Test
    void publishesCommentEvenWhenNoUserNotificationIsNeeded() {
        when(comment.getParentComment()).thenReturn(null);
        when(task.getAssigneeUser()).thenReturn(null);

        service.notifyCommentCreated(task, comment, actor, Set.of());

        verify(outboundWebhookEventService)
                .publish(
                        eq(tenantId),
                        eq(OutboundWebhookEventType.COMMENT_CREATED),
                        any(OutboundWebhookCommentPayload.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void publishesReplyEvenWhenParentAuthorIsTheActor() {
        UUID actorId = UUID.randomUUID();
        when(actor.getId()).thenReturn(actorId);
        when(parent.getAuthorUser()).thenReturn(actor);
        when(parent.getId()).thenReturn(UUID.randomUUID());
        when(comment.getParentComment()).thenReturn(parent);

        service.notifyReplyCreated(task, comment, parent, actor, Set.of());

        verify(outboundWebhookEventService)
                .publish(
                        eq(tenantId),
                        eq(OutboundWebhookEventType.COMMENT_REPLIED),
                        any(OutboundWebhookCommentPayload.class));
        verifyNoInteractions(notificationService);
    }
}
