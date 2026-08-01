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
import { AuditLogsPage } from '../pages/AuditLogsPage'
import { DashboardPage } from '../pages/DashboardPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ProjectsPage } from '../pages/ProjectsPage'
import { ProjectDetailsPage } from '../pages/ProjectDetailsPage'
import { UsersPage } from '../pages/UsersPage'

export function AppRoutes() {
    return (
        <Routes>
            <Route element={<PublicOnlyRoute />}>
                <Route
                    path="login"
                    element={<LoginPage />}
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
