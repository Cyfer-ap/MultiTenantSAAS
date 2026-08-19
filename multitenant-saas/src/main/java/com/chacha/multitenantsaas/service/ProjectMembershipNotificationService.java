package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProjectMembershipNotificationService {

    private static final Set<NotificationDeliveryChannel> EMAIL_DELIVERY =
            Set.of(NotificationDeliveryChannel.EMAIL);

    private final NotificationService notificationService;

    public ProjectMembershipNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifyAdded(
            Project project, AppUser actor, AppUser member, ProjectMemberRole role) {
        if (!shouldNotify(member, actor)) {
            return;
        }

        notificationService.create(
                project.getTenant(),
                member,
                NotificationType.PROJECT_MEMBERSHIP_CHANGED,
                "Added to a project",
                actor.getFullName()
                        + " added you to \""
                        + project.getName()
                        + "\" as "
                        + displayRole(role)
                        + ".",
                projectTarget(project),
                EMAIL_DELIVERY);
    }

    public void notifyRoleChanged(
            Project project,
            AppUser actor,
            AppUser member,
            ProjectMemberRole previousRole,
            ProjectMemberRole newRole) {
        if (previousRole == newRole || !shouldNotify(member, actor)) {
            return;
        }

        notificationService.create(
                project.getTenant(),
                member,
                NotificationType.PROJECT_MEMBERSHIP_CHANGED,
                "Project role changed",
                actor.getFullName()
                        + " changed your role in \""
                        + project.getName()
                        + "\" from "
                        + displayRole(previousRole)
                        + " to "
                        + displayRole(newRole)
                        + ".",
                projectTarget(project),
                EMAIL_DELIVERY);
    }

    public void notifyRemoved(Project project, AppUser actor, AppUser member) {
        if (!shouldNotify(member, actor)) {
            return;
        }

        notificationService.create(
                project.getTenant(),
                member,
                NotificationType.PROJECT_MEMBERSHIP_CHANGED,
                "Removed from a project",
                actor.getFullName() + " removed you from \"" + project.getName() + "\".",
                "/projects",
                EMAIL_DELIVERY);
    }

    private boolean shouldNotify(AppUser recipient, AppUser actor) {
        return recipient != null
                && actor != null
                && recipient.getId() != null
                && actor.getId() != null
                && !recipient.getId().equals(actor.getId());
    }

    private String projectTarget(Project project) {
        return "/projects/" + project.getId();
    }

    private String displayRole(ProjectMemberRole role) {
        return role.name().toLowerCase().replace('_', ' ');
    }
}
