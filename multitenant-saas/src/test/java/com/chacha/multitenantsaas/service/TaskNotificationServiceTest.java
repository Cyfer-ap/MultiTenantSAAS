package com.chacha.multitenantsaas.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskComment;
import com.chacha.multitenantsaas.entity.Tenant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskNotificationServiceTest {

    @Mock private NotificationService notificationService;
    @Mock private Tenant tenant;
    @Mock private Project project;
    @Mock private ProjectTask task;
    @Mock private AppUser actor;
    @Mock private AppUser assignee;
    @Mock private AppUser mentionedUser;
    @Mock private AppUser parentAuthor;
    @Mock private TaskComment comment;
    @Mock private TaskComment reply;
    @Mock private TaskComment parent;

    private TaskNotificationService service;

    private UUID projectId;
    private UUID taskId;
    private UUID actorId;
    private UUID assigneeId;

    @BeforeEach
    void setUp() {
        service = new TaskNotificationService(notificationService);
        projectId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        when(task.getTenant()).thenReturn(tenant);
        when(task.getProject()).thenReturn(project);
        when(task.getId()).thenReturn(taskId);
        when(task.getTitle()).thenReturn("Review access controls");
        when(project.getId()).thenReturn(projectId);
        lenient().when(project.getName()).thenReturn("Security Project");
        when(actor.getId()).thenReturn(actorId);
        when(actor.getFullName()).thenReturn("Ada Admin");
    }

    @Test
    void preservesAssignmentNotificationAndEmailDelivery() {
        when(assignee.getId()).thenReturn(assigneeId);

        service.notifyAssignment(task, actor, assignee);

        verify(notificationService)
                .create(
                        tenant,
                        assignee,
                        NotificationType.TASK_ASSIGNED,
                        "You were assigned a task",
                        "Ada Admin assigned \"Review access controls\" to you in Security Project.",
                        "/projects/" + projectId + "?task=" + taskId,
                        Set.of(NotificationDeliveryChannel.EMAIL));
    }

    @Test
    void mentionTakesPrecedenceOverGenericAssigneeCommentNotification() {
        UUID commentId = UUID.randomUUID();
        when(mentionedUser.getId()).thenReturn(assigneeId);
        when(task.getAssigneeUser()).thenReturn(mentionedUser);
        when(comment.getId()).thenReturn(commentId);
        when(comment.getParentComment()).thenReturn(null);

        service.notifyCommentCreated(task, comment, actor, Set.of(mentionedUser));

        verify(notificationService)
                .create(
                        tenant,
                        mentionedUser,
                        NotificationType.TASK_COMMENT_MENTIONED,
                        "You were mentioned in a task comment",
                        "Ada Admin mentioned you on \"Review access controls\" in Security Project.",
                        "/projects/"
                                + projectId
                                + "?task="
                                + taskId
                                + "&comment="
                                + commentId,
                        Set.of(NotificationDeliveryChannel.EMAIL));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void replyNotifiesDistinctMentionRecipientAndParentAuthor() {
        UUID parentId = UUID.randomUUID();
        UUID replyId = UUID.randomUUID();
        UUID parentAuthorId = UUID.randomUUID();
        UUID otherMentionId = UUID.randomUUID();

        when(parent.getAuthorUser()).thenReturn(parentAuthor);
        when(parent.getId()).thenReturn(parentId);
        when(parentAuthor.getId()).thenReturn(parentAuthorId);
        when(mentionedUser.getId()).thenReturn(otherMentionId);
        when(reply.getId()).thenReturn(replyId);
        when(reply.getParentComment()).thenReturn(parent);

        service.notifyReplyCreated(task, reply, parent, actor, Set.of(mentionedUser));

        verify(notificationService)
                .create(
                        eq(tenant),
                        eq(mentionedUser),
                        eq(NotificationType.TASK_COMMENT_MENTIONED),
                        eq("You were mentioned in a task comment"),
                        eq(
                                "Ada Admin mentioned you on \"Review access controls\" in Security Project."),
                        eq(
                                "/projects/"
                                        + projectId
                                        + "?task="
                                        + taskId
                                        + "&comment="
                                        + parentId
                                        + "&reply="
                                        + replyId),
                        eq(Set.of(NotificationDeliveryChannel.EMAIL)));
        verify(notificationService)
                .create(
                        eq(tenant),
                        eq(parentAuthor),
                        eq(NotificationType.TASK_COMMENT_REPLIED),
                        eq("New reply to your comment"),
                        eq("Ada Admin replied to your comment on \"Review access controls\"."),
                        eq(
                                "/projects/"
                                        + projectId
                                        + "?task="
                                        + taskId
                                        + "&comment="
                                        + parentId
                                        + "&reply="
                                        + replyId),
                        eq(Set.of(NotificationDeliveryChannel.EMAIL)));
        verify(notificationService, times(2))
                .create(
                        eq(tenant),
                        org.mockito.ArgumentMatchers.any(AppUser.class),
                        org.mockito.ArgumentMatchers.any(NotificationType.class),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        eq(Set.of(NotificationDeliveryChannel.EMAIL)));
    }

    @Test
    void doesNotDoubleNotifyReplyAuthorWhenTheyAreMentioned() {
        UUID parentId = UUID.randomUUID();
        UUID replyId = UUID.randomUUID();
        UUID parentAuthorId = UUID.randomUUID();

        when(parent.getAuthorUser()).thenReturn(parentAuthor);
        when(parent.getId()).thenReturn(parentId);
        when(parentAuthor.getId()).thenReturn(parentAuthorId);
        when(reply.getId()).thenReturn(replyId);
        when(reply.getParentComment()).thenReturn(parent);

        service.notifyReplyCreated(task, reply, parent, actor, Set.of(parentAuthor));

        verify(notificationService)
                .create(
                        eq(tenant),
                        eq(parentAuthor),
                        eq(NotificationType.TASK_COMMENT_MENTIONED),
                        eq("You were mentioned in a task comment"),
                        eq(
                                "Ada Admin mentioned you on \"Review access controls\" in Security Project."),
                        eq(
                                "/projects/"
                                        + projectId
                                        + "?task="
                                        + taskId
                                        + "&comment="
                                        + parentId
                                        + "&reply="
                                        + replyId),
                        eq(Set.of(NotificationDeliveryChannel.EMAIL)));
        verify(notificationService, never())
                .create(
                        eq(tenant),
                        eq(parentAuthor),
                        eq(NotificationType.TASK_COMMENT_REPLIED),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        eq(Set.of(NotificationDeliveryChannel.EMAIL)));
    }

    @Test
    void notifiesAssigneeForRealStatusChangesButSuppressesSelfAndNoOpChanges() {
        when(task.getAssigneeUser()).thenReturn(assignee);
        when(assignee.getId()).thenReturn(assigneeId);

        service.notifyStatusChanged(
                task, actor, ProjectTaskStatus.TODO, ProjectTaskStatus.IN_PROGRESS);

        verify(notificationService)
                .create(
                        tenant,
                        assignee,
                        NotificationType.TASK_STATUS_CHANGED,
                        "Task status changed",
                        "Ada Admin changed \"Review access controls\" from todo to in progress.",
                        "/projects/" + projectId + "?task=" + taskId,
                        Set.of(NotificationDeliveryChannel.EMAIL));

        org.mockito.Mockito.reset(notificationService);
        service.notifyStatusChanged(
                task, actor, ProjectTaskStatus.IN_PROGRESS, ProjectTaskStatus.IN_PROGRESS);
        verifyNoInteractions(notificationService);

        org.mockito.Mockito.reset(notificationService);
        when(assignee.getId()).thenReturn(actorId);
        service.notifyStatusChanged(
                task, actor, ProjectTaskStatus.IN_PROGRESS, ProjectTaskStatus.COMPLETED);
        verifyNoInteractions(notificationService);
    }
}
