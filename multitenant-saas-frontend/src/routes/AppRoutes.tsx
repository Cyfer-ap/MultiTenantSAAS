import {
    Navigate,
    Route,
    Routes,
} from 'react-router'

import { AppShell } from '../layouts/AppShell'
import { AuditLogsPage } from '../pages/AuditLogsPage'
import { DashboardPage } from '../pages/DashboardPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { ProjectsPage } from '../pages/ProjectsPage'
import { UsersPage } from '../pages/UsersPage'

export function AppRoutes() {
    return (
        <Routes>
            <Route element={<AppShell />}>
                <Route
                    index
                    element={
                        <Navigate
                            to="/dashboard"
                            replace
                        />
                    }
                />

                <Route
                    path="dashboard"
                    element={<DashboardPage />}
                />

                <Route
                    path="users"
                    element={<UsersPage />}
                />

                <Route
                    path="projects"
                    element={<ProjectsPage />}
                />

                <Route
                    path="audit-logs"
                    element={<AuditLogsPage />}
                />

                <Route
                    path="*"
                    element={<NotFoundPage />}
                />
            </Route>
        </Routes>
    )
}