import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router'

import { ProtectedRoute, PublicOnlyRoute } from '../features/auth/routing/AuthRoutes'
import { dashboardRequiredTenantPermissions } from '../features/authorization/access/authorizationAccess'
import {
    AuthorizationHomeRedirect,
    ProjectPermissionProtectedRoute,
    TenantPermissionProtectedRoute,
} from '../features/authorization/routing/AuthorizationRoutes'
import { authorizationPermissionCodes } from '../features/authorization/types/authorization'
import {
    SystemHomeRedirect,
    SystemProtectedRoute,
    SystemPublicOnlyRoute,
} from '../features/system-admin/routing/SystemAdminRoutes'

const LoginPage = lazy(() =>
    import('../features/auth/pages/LoginPage').then((module) => ({
        default: module.LoginPage,
    })),
)

const AppShell = lazy(() =>
    import('../layouts/AppShell').then((module) => ({
        default: module.AppShell,
    })),
)

const SystemAdminShell = lazy(() =>
    import('../layouts/SystemAdminShell').then((module) => ({
        default: module.SystemAdminShell,
    })),
)

const AcceptInvitationPage = lazy(() =>
    import('../pages/AcceptInvitationPage').then((module) => ({
        default: module.AcceptInvitationPage,
    })),
)

const AccountSettingsPage = lazy(() =>
    import('../pages/AccountSettingsPage').then((module) => ({
        default: module.AccountSettingsPage,
    })),
)

const AuditLogsPage = lazy(() =>
    import('../pages/AuditLogsPage').then((module) => ({
        default: module.AuditLogsPage,
    })),
)

const AuthorizationManagementPage = lazy(() =>
    import('../pages/AuthorizationManagementPage').then((module) => ({
        default: module.AuthorizationManagementPage,
    })),
)

const DashboardPage = lazy(() =>
    import('../pages/DashboardPage').then((module) => ({
        default: module.DashboardPage,
    })),
)

const ForgotPasswordPage = lazy(() =>
    import('../pages/ForgotPasswordPage').then((module) => ({
        default: module.ForgotPasswordPage,
    })),
)

const InvitationsPage = lazy(() =>
    import('../pages/InvitationsPage').then((module) => ({
        default: module.InvitationsPage,
    })),
)

const NotFoundPage = lazy(() =>
    import('../pages/NotFoundPage').then((module) => ({
        default: module.NotFoundPage,
    })),
)

const OrganizationPage = lazy(() =>
    import('../pages/OrganizationPage').then((module) => ({
        default: module.OrganizationPage,
    })),
)

const PlatformAuditLogsPage = lazy(() =>
    import('../pages/PlatformAuditLogsPage').then((module) => ({
        default: module.PlatformAuditLogsPage,
    })),
)

const ProjectDetailsPage = lazy(() =>
    import('../pages/ProjectDetailsPage').then((module) => ({
        default: module.ProjectDetailsPage,
    })),
)

const ProjectsPage = lazy(() =>
    import('../pages/ProjectsPage').then((module) => ({
        default: module.ProjectsPage,
    })),
)

const ResetPasswordPage = lazy(() =>
    import('../pages/ResetPasswordPage').then((module) => ({
        default: module.ResetPasswordPage,
    })),
)

const SystemAdminsPage = lazy(() =>
    import('../pages/SystemAdminsPage').then((module) => ({
        default: module.SystemAdminsPage,
    })),
)

const SystemChangePasswordPage = lazy(() =>
    import('../pages/SystemChangePasswordPage').then((module) => ({
        default: module.SystemChangePasswordPage,
    })),
)

const SystemDashboardPage = lazy(() =>
    import('../pages/SystemDashboardPage').then((module) => ({
        default: module.SystemDashboardPage,
    })),
)

const SystemLoginPage = lazy(() =>
    import('../pages/SystemLoginPage').then((module) => ({
        default: module.SystemLoginPage,
    })),
)

const SystemSubscriptionsPage = lazy(() =>
    import('../pages/SystemSubscriptionsPage').then((module) => ({
        default: module.SystemSubscriptionsPage,
    })),
)

const SystemTenantsPage = lazy(() =>
    import('../pages/SystemTenantsPage').then((module) => ({
        default: module.SystemTenantsPage,
    })),
)

const TenantChangePasswordPage = lazy(() =>
    import('../pages/TenantChangePasswordPage').then((module) => ({
        default: module.TenantChangePasswordPage,
    })),
)

const TenantOnboardingPage = lazy(() =>
    import('../pages/TenantOnboardingPage').then((module) => ({
        default: module.TenantOnboardingPage,
    })),
)

const TenantSubscriptionPage = lazy(() =>
    import('../pages/TenantSubscriptionPage').then((module) => ({
        default: module.TenantSubscriptionPage,
    })),
)

const UsersPage = lazy(() =>
    import('../pages/UsersPage').then((module) => ({
        default: module.UsersPage,
    })),
)

export function AppRoutes() {
    return (
        <Suspense fallback={<div aria-live="polite">Loading...</div>}>
            <Routes>
                <Route element={<SystemPublicOnlyRoute />}>
                    <Route path="system/login" element={<SystemLoginPage />} />
                </Route>

                <Route element={<SystemProtectedRoute />}>
                    <Route path="system" element={<SystemAdminShell />}>
                        <Route index element={<SystemHomeRedirect />} />
                        <Route path="dashboard" element={<SystemDashboardPage />} />
                        <Route path="tenants" element={<SystemTenantsPage />} />
                        <Route path="subscriptions" element={<SystemSubscriptionsPage />} />
                        <Route path="admins" element={<SystemAdminsPage />} />
                        <Route path="audit-logs" element={<PlatformAuditLogsPage />} />
                        <Route path="change-password" element={<SystemChangePasswordPage />} />
                        <Route path="*" element={<NotFoundPage />} />
                    </Route>
                </Route>

                <Route element={<PublicOnlyRoute />}>
                    <Route path="login" element={<LoginPage />} />

                    <Route path="accept-invitation" element={<AcceptInvitationPage />} />

                    <Route path="forgot-password" element={<ForgotPasswordPage />} />

                    <Route path="reset-password" element={<ResetPasswordPage />} />

                    <Route path="register" element={<TenantOnboardingPage />} />
                </Route>

                <Route element={<ProtectedRoute />}>
                    <Route element={<AppShell />}>
                        <Route index element={<AuthorizationHomeRedirect />} />

                        <Route
                            element={
                                <TenantPermissionProtectedRoute
                                    requiredPermissions={dashboardRequiredTenantPermissions}
                                />
                            }
                        >
                            <Route path="dashboard" element={<DashboardPage />} />
                        </Route>

                        <Route
                            element={
                                <TenantPermissionProtectedRoute
                                    requiredPermissions={[authorizationPermissionCodes.USER_READ]}
                                />
                            }
                        >
                            <Route path="users" element={<UsersPage />} />
                        </Route>

                        <Route
                            element={
                                <TenantPermissionProtectedRoute
                                    requiredPermissions={[authorizationPermissionCodes.USER_READ]}
                                />
                            }
                        >
                            <Route path="invitations" element={<InvitationsPage />} />
                        </Route>

                        <Route
                            element={
                                <TenantPermissionProtectedRoute
                                    requiredPermissions={[
                                        authorizationPermissionCodes.ORGANIZATION_UNIT_READ,
                                    ]}
                                />
                            }
                        >
                            <Route path="organization" element={<OrganizationPage />} />
                        </Route>

                        <Route
                            element={
                                <TenantPermissionProtectedRoute
                                    requiredPermissions={[
                                        authorizationPermissionCodes.AUTHORIZATION_MANAGE,
                                    ]}
                                />
                            }
                        >
                            <Route path="authorization" element={<AuthorizationManagementPage />} />
                        </Route>

                        <Route
                            element={
                                <TenantPermissionProtectedRoute
                                    requiredPermissions={[authorizationPermissionCodes.AUDIT_READ]}
                                />
                            }
                        >
                            <Route path="audit-logs" element={<AuditLogsPage />} />
                        </Route>

                        <Route
                            element={
                                <TenantPermissionProtectedRoute
                                    requiredPermissions={[
                                        authorizationPermissionCodes.PROJECT_READ,
                                    ]}
                                />
                            }
                        >
                            <Route path="projects" element={<ProjectsPage />} />
                        </Route>

                        <Route
                            element={
                                <ProjectPermissionProtectedRoute
                                    permissionCode={authorizationPermissionCodes.PROJECT_READ}
                                />
                            }
                        >
                            <Route path="projects/:projectId" element={<ProjectDetailsPage />} />
                        </Route>

                        <Route
                            element={
                                <TenantPermissionProtectedRoute
                                    requiredPermissions={[
                                        authorizationPermissionCodes.SUBSCRIPTION_READ,
                                    ]}
                                />
                            }
                        >
                            <Route path="subscription" element={<TenantSubscriptionPage />} />
                        </Route>

                        <Route path="account" element={<AccountSettingsPage />} />

                        <Route
                            path="account/change-password"
                            element={<TenantChangePasswordPage />}
                        />

                        <Route path="*" element={<NotFoundPage />} />
                    </Route>
                </Route>
            </Routes>
        </Suspense>
    )
}
