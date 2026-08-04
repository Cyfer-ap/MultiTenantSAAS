import {
    Route,
    Routes,
} from 'react-router'

import { LoginPage } from '../features/auth/pages/LoginPage'
import {
    ProtectedRoute,
    PublicOnlyRoute,
} from '../features/auth/routing/AuthRoutes'
import {
    dashboardRequiredTenantPermissions,
} from '../features/authorization/access/authorizationAccess'
import {
    AuthorizationHomeRedirect,
    ProjectPermissionProtectedRoute,
    TenantPermissionProtectedRoute,
} from '../features/authorization/routing/AuthorizationRoutes'
import {
    authorizationPermissionCodes,
} from '../features/authorization/types/authorization'
import {
    SystemHomeRedirect,
    SystemProtectedRoute,
    SystemPublicOnlyRoute,
} from '../features/system-admin/routing/SystemAdminRoutes'
import { AppShell } from '../layouts/AppShell'
import { SystemAdminShell } from '../layouts/SystemAdminShell'
import { AcceptInvitationPage } from '../pages/AcceptInvitationPage'
import { AccountSettingsPage } from '../pages/AccountSettingsPage'
import { AuditLogsPage } from '../pages/AuditLogsPage'
import { DashboardPage } from '../pages/DashboardPage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { InvitationsPage } from '../pages/InvitationsPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { PlatformAuditLogsPage } from '../pages/PlatformAuditLogsPage'
import { ProjectDetailsPage } from '../pages/ProjectDetailsPage'
import { ProjectsPage } from '../pages/ProjectsPage'
import { ResetPasswordPage } from '../pages/ResetPasswordPage'
import { SystemAdminsPage } from '../pages/SystemAdminsPage'
import { SystemChangePasswordPage } from '../pages/SystemChangePasswordPage'
import { SystemDashboardPage } from '../pages/SystemDashboardPage'
import { SystemLoginPage } from '../pages/SystemLoginPage'
import { SystemTenantsPage } from '../pages/SystemTenantsPage'
import { TenantChangePasswordPage } from '../pages/TenantChangePasswordPage'
import { TenantOnboardingPage } from '../pages/TenantOnboardingPage'
import { UsersPage } from '../pages/UsersPage'

export function AppRoutes() {
    return (
        <Routes>
            <Route element={<SystemPublicOnlyRoute />}>
                <Route
                    path="system/login"
                    element={<SystemLoginPage />}
                />
            </Route>

            <Route element={<SystemProtectedRoute />}>
                <Route
                    path="system"
                    element={<SystemAdminShell />}
                >
                    <Route
                        index
                        element={<SystemHomeRedirect />}
                    />
                    <Route
                        path="dashboard"
                        element={<SystemDashboardPage />}
                    />
                    <Route
                        path="tenants"
                        element={<SystemTenantsPage />}
                    />
                    <Route
                        path="admins"
                        element={<SystemAdminsPage />}
                    />
                    <Route
                        path="audit-logs"
                        element={<PlatformAuditLogsPage />}
                    />
                    <Route
                        path="change-password"
                        element={<SystemChangePasswordPage />}
                    />
                    <Route
                        path="*"
                        element={<NotFoundPage />}
                    />
                </Route>
            </Route>

            <Route element={<PublicOnlyRoute />}>
                <Route
                    path="login"
                    element={<LoginPage />}
                />

                <Route
                    path="accept-invitation"
                    element={<AcceptInvitationPage />}
                />

                <Route
                    path="forgot-password"
                    element={<ForgotPasswordPage />}
                />

                <Route
                    path="reset-password"
                    element={<ResetPasswordPage />}
                />

                <Route
                    path="register"
                    element={<TenantOnboardingPage />}
                />
            </Route>

            <Route element={<ProtectedRoute />}>
                <Route element={<AppShell />}>
                    <Route
                        index
                        element={<AuthorizationHomeRedirect />}
                    />

                    <Route
                        element={
                            <TenantPermissionProtectedRoute
                                requiredPermissions={
                                    dashboardRequiredTenantPermissions
                                }
                            />
                        }
                    >
                        <Route
                            path="dashboard"
                            element={<DashboardPage />}
                        />
                    </Route>

                    <Route
                        element={
                            <TenantPermissionProtectedRoute
                                requiredPermissions={[
                                    authorizationPermissionCodes
                                        .USER_READ,
                                ]}
                            />
                        }
                    >
                        <Route
                            path="users"
                            element={<UsersPage />}
                        />
                    </Route>

                    <Route
                        element={
                            <TenantPermissionProtectedRoute
                                requiredPermissions={[
                                    authorizationPermissionCodes
                                        .USER_READ,
                                ]}
                            />
                        }
                    >
                        <Route
                            path="invitations"
                            element={<InvitationsPage />}
                        />
                    </Route>

                    <Route
                        element={
                            <TenantPermissionProtectedRoute
                                requiredPermissions={[
                                    authorizationPermissionCodes
                                        .AUDIT_READ,
                                ]}
                            />
                        }
                    >
                        <Route
                            path="audit-logs"
                            element={<AuditLogsPage />}
                        />
                    </Route>

                    <Route
                        element={
                            <TenantPermissionProtectedRoute
                                requiredPermissions={[
                                    authorizationPermissionCodes
                                        .PROJECT_READ,
                                ]}
                            />
                        }
                    >
                        <Route
                            path="projects"
                            element={<ProjectsPage />}
                        />
                    </Route>

                    <Route
                        element={
                            <ProjectPermissionProtectedRoute
                                permissionCode={
                                    authorizationPermissionCodes
                                        .PROJECT_READ
                                }
                            />
                        }
                    >
                        <Route
                            path="projects/:projectId"
                            element={<ProjectDetailsPage />}
                        />
                    </Route>

                    <Route
                        path="account"
                        element={<AccountSettingsPage />}
                    />

                    <Route
                        path="account/change-password"
                        element={<TenantChangePasswordPage />}
                    />

                    <Route
                        path="*"
                        element={<NotFoundPage />}
                    />
                </Route>
            </Route>
        </Routes>
    )
}
