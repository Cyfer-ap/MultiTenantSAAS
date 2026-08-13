package com.chacha.multitenantsaas.repository;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.PlatformAuditAction;
import com.chacha.multitenantsaas.entity.PlatformAuditLog;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectMember;
import com.chacha.multitenantsaas.entity.ProjectMemberRole;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserInvitation;
import com.chacha.multitenantsaas.entity.UserInvitationStatus;
import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.entity.UserStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class FilterSpecifications {

    private FilterSpecifications() {}

    public static Specification<Tenant> tenants(TenantStatus status, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), pattern),
                                cb.like(cb.lower(root.get("slug")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Project> projects(
            UUID tenantId, ProjectStatus status, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), pattern),
                                cb.like(
                                        cb.lower(cb.coalesce(root.<String>get("description"), "")),
                                        pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<AppUser> users(
            UUID tenantId, UserRole role, UserStatus status, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("fullName")), pattern),
                                cb.like(cb.lower(root.get("email")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<UserInvitation> invitations(
            UUID tenantId, UserInvitationStatus status, UserRole role, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("fullName")), pattern),
                                cb.like(cb.lower(root.get("email")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<SystemAdmin> systemAdmins(UserStatus status, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("fullName")), pattern),
                                cb.like(cb.lower(root.get("email")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<PlatformAuditLog> platformAuditLogs(
            PlatformAuditAction action, Boolean success, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            if (success != null) {
                predicates.add(cb.equal(root.get("success"), success));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                var actor = root.join("actorSystemAdmin", JoinType.LEFT);

                var target = root.join("targetSystemAdmin", JoinType.LEFT);

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(actor.get("email")), pattern),
                                cb.like(cb.lower(target.get("email")), pattern),
                                cb.like(cb.lower(root.get("message")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<ProjectMember> projectMembers(
            UUID tenantId, UUID projectId, ProjectMemberRole role, String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("project").get("tenant").get("id"), tenantId));

            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                var user = root.join("user");

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(user.get("fullName")), pattern),
                                cb.like(cb.lower(user.get("email")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<ProjectTask> projectTasks(
            UUID tenantId,
            UUID projectId,
            ProjectTaskStatus status,
            ProjectTaskPriority priority,
            UUID assigneeUserId,
            String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (assigneeUserId != null) {
                predicates.add(cb.equal(root.get("assigneeUser").get("id"), assigneeUserId));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("title")), pattern),
                                cb.like(
                                        cb.lower(cb.coalesce(root.<String>get("description"), "")),
                                        pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
