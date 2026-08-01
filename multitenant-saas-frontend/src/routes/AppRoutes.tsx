import {
    Route,
    Routes,
} from 'react-router'

import {
    tenantAdminRoles,
    tenantManagementRoles,
} from '../features/auth/access/roleAccess'
import { LoginPage } from '../features/auth/pages/LoginPage'
import {
    AuthenticatedHomeRedirect,
    ProtectedRoute,
    PublicOnlyRoute,
    RoleProtectedRoute,
} from '../features/auth/routing/AuthRoutes'
import { AppShell } from '../layouts/AppShell'
import { AcceptInvitationPage } from '../pages/AcceptInvitationPage'
import { AuditLogsPage } from '../pages/AuditLogsPage'
import { DashboardPage } from '../pages/DashboardPage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { InvitationsPage } from '../pages/InvitationsPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ProjectsPage } from '../pages/ProjectsPage'
import { ProjectDetailsPage } from '../pages/ProjectDetailsPage'
import { ResetPasswordPage } from '../pages/ResetPasswordPage'
import { UsersPage } from '../pages/UsersPage'

export function AppRoutes() {
    return (
        <Routes>
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
            </Route>

            <Route element={<ProtectedRoute />}>
                <Route element={<AppShell />}>
                    <Route
                        index
                        element={<AuthenticatedHomeRedirect />}
                    />

                    <Route
                        element={
                            <RoleProtectedRoute
                                allowedRoles={
                                    tenantManagementRoles
                                }
                            />
                        }
                    >
                        <Route
                            path="dashboard"
                            element={<DashboardPage />}
                        />

                        <Route
                            path="users"
                            element={<UsersPage />}
                        />
                    </Route>

                    <Route
                        element={
                            <RoleProtectedRoute
                                allowedRoles={
                                    tenantAdminRoles
                                }
                            />
                        }
                    >
                        <Route
                            path="invitations"
                            element={<InvitationsPage />}
                        />

                        <Route
                            path="audit-logs"
                            element={<AuditLogsPage />}
                        />
                    </Route>

                    <Route
                        path="projects"
                        element={<ProjectsPage />}
                    />

                    <Route
                        path="projects/:projectId"
                        element={<ProjectDetailsPage />}
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
