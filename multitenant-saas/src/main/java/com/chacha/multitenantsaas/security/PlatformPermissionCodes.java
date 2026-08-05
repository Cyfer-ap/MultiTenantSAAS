package com.chacha.multitenantsaas.security;

public final class PlatformPermissionCodes {

    public static final String TENANT_READ =
            "tenant.read";

    public static final String TENANT_UPDATE =
            "tenant.update";

    public static final String USER_READ =
            "user.read";

    public static final String USER_CREATE =
            "user.create";

    public static final String USER_UPDATE =
            "user.update";

    public static final String USER_STATUS_UPDATE =
            "user.status.update";

    public static final String ORGANIZATION_UNIT_READ =
            "organization.unit.read";

    public static final String ORGANIZATION_UNIT_MANAGE =
            "organization.unit.manage";

    public static final String ORGANIZATION_ASSIGNMENT_READ =
            "organization.assignment.read";

    public static final String ORGANIZATION_ASSIGNMENT_MANAGE =
            "organization.assignment.manage";

    public static final String PROJECT_READ =
            "project.read";

    public static final String PROJECT_CREATE =
            "project.create";

    public static final String PROJECT_UPDATE =
            "project.update";

    public static final String PROJECT_ARCHIVE =
            "project.archive";

    public static final String PROJECT_MEMBER_MANAGE =
            "project.member.manage";

    public static final String PROJECT_TASK_READ =
            "project.task.read";

    public static final String PROJECT_TASK_MANAGE =
            "project.task.manage";

    public static final String AUDIT_READ =
            "audit.read";

    public static final String AUTHORIZATION_MANAGE =
            "authorization.manage";

    public static final String SUBSCRIPTION_READ =
            "subscription.read";

    private PlatformPermissionCodes() {
    }
}
