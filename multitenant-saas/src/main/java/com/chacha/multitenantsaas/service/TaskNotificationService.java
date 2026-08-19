package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskComment;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TaskNotificationService {

    private static final Set<NotificationDeliveryChannel> EMAIL_DELIVERY =
            Set.of(NotificationDeliveryChannel.EMAIL);

    private final NotificationService notificationService;

    public TaskNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifyAssignment(ProjectTask task, AppUser actor, AppUser assignee) {
        if (!shouldNotify(assignee, actor)) {
            return;
        }

        notificationService.create(
                task.getTenant(),
                assignee,
                NotificationType.TASK_ASSIGNED,
                "You were assigned a task",
                actor.getFullName()
                        + " assigned \""
                        + task.getTitle()
                        + "\" to you in "
                        + task.getProject().getName()
                        + ".",
                taskTarget(task),
                EMAIL_DELIVERY);
    }

    public void notifyCommentCreated(
            ProjectTask task,
            TaskComment comment,
            AppUser actor,
            Set<AppUser> mentionedUsers) {
        Set<UUID> notifiedUserIds = notifyMentions(task, comment, actor, mentionedUsers);
        AppUser assignee = task.getAssigneeUser();

        if (shouldNotify(assignee, actor) && !notifiedUserIds.contains(assignee.getId())) {
            notificationService.create(
                    task.getTenant(),
                    assignee,
                    NotificationType.TASK_COMMENT_ADDED,
                    "New comment on your task",
                    actor.getFullName()
                            + " commented on \""
                            + task.getTitle()
                            + "\" in "
                            + task.getProject().getName()
                            + ".",
                    commentTarget(task, comment),
                    EMAIL_DELIVERY);
        }
    }

    public void notifyReplyCreated(
            ProjectTask task,
            TaskComment reply,
            TaskComment parent,
            AppUser actor,
            Set<AppUser> mentionedUsers) {
        Set<UUID> notifiedUserIds = notifyMentions(task, reply, actor, mentionedUsers);
        AppUser parentAuthor = parent.getAuthorUser();

        if (shouldNotify(parentAuthor, actor)
                && !notifiedUserIds.contains(parentAuthor.getId())) {
            notificationService.create(
                    task.getTenant(),
                    parentAuthor,
                    NotificationType.TASK_COMMENT_REPLIED,
                    "New reply to your comment",
                    actor.getFullName()
                            + " replied to your comment on \""
                            + task.getTitle()
                            + "\".",
                    commentTarget(task, reply),
                    EMAIL_DELIVERY);
        }
    }

    public void notifyMentionedUsers(
            ProjectTask task, TaskComment comment, AppUser actor, Set<AppUser> mentionedUsers) {
        notifyMentions(task, comment, actor, mentionedUsers);
    }

    public void notifyStatusChanged(
            ProjectTask task,
            AppUser actor,
            ProjectTaskStatus previousStatus,
            ProjectTaskStatus newStatus) {
        AppUser assignee = task.getAssigneeUser();
        if (previousStatus == newStatus || !shouldNotify(assignee, actor)) {
            return;
        }

        notificationService.create(
                task.getTenant(),
                assignee,
                NotificationType.TASK_STATUS_CHANGED,
                "Task status changed",
                actor.getFullName()
                        + " changed \""
                        + task.getTitle()
                        + "\" from "
                        + displayStatus(previousStatus)
                        + " to "
                        + displayStatus(newStatus)
                        + ".",
                taskTarget(task),
                EMAIL_DELIVERY);
    }

    private Set<UUID> notifyMentions(
            ProjectTask task, TaskComment comment, AppUser actor, Set<AppUser> mentionedUsers) {
        Set<UUID> notifiedUserIds = new LinkedHashSet<>();
        for (AppUser mentionedUser : mentionedUsers) {
            if (!shouldNotify(mentionedUser, actor) || !notifiedUserIds.add(mentionedUser.getId())) {
                continue;
            }

            notificationService.create(
                    task.getTenant(),
                    mentionedUser,
                    NotificationType.TASK_COMMENT_MENTIONED,
                    "You were mentioned in a task comment",
                    actor.getFullName()
                            + " mentioned you on \""
                            + task.getTitle()
                            + "\" in "
                            + task.getProject().getName()
                            + ".",
                    commentTarget(task, comment),
                    EMAIL_DELIVERY);
        }
        return notifiedUserIds;
    }

    private boolean shouldNotify(AppUser recipient, AppUser actor) {
        return recipient != null
                && actor != null
                && recipient.getId() != null
                && actor.getId() != null
                && !recipient.getId().equals(actor.getId());
    }

    private String taskTarget(ProjectTask task) {
        return "/projects/" + task.getProject().getId() + "?task=" + task.getId();
    }

    private String commentTarget(ProjectTask task, TaskComment comment) {
        TaskComment parent = comment.getParentComment();
        if (parent == null) {
            return taskTarget(task) + "&comment=" + comment.getId();
        }
        return taskTarget(task)
                + "&comment="
                + parent.getId()
                + "&reply="
                + comment.getId();
    }

    private String displayStatus(ProjectTaskStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }
}
