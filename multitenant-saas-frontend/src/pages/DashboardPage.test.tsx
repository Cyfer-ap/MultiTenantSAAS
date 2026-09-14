import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { dashboardApi } from '../features/dashboard/api/dashboardApi'
import type { TenantDashboardSummary } from '../features/dashboard/types/dashboard'
import { myWorkApi } from '../features/my-work/api/myWorkApi'
import { personalWorkspaceApi } from '../features/personal-workspace/api/personalWorkspaceApi'
import { appTheme } from '../theme/appTheme'
import { DashboardPage } from './DashboardPage'

vi.mock('../features/authorization/hooks/useCurrentAuthorization', () => ({
    useCurrentAuthorization: () => ({
        data: {
            tenantId: 'tenant-id',
            userId: 'user-id',
            fullName: 'Example User',
            email: 'user@example.com',
            evaluatedAt: '2026-09-14T12:00:00Z',
            tenantPermissionCodes: ['project.read', 'user.read'],
            allPermissionCodes: ['project.read', 'user.read'],
            grants: [],
        },
    }),
}))

const dashboardSummary: TenantDashboardSummary = {
    tenantId: 'tenant-id',
    tenantName: 'Example Tenant',
    tenantSlug: 'example-tenant',
    tenantStatus: 'ACTIVE',
    totalUsers: 8,
    activeUsers: 6,
    inactiveUsers: 1,
    suspendedUsers: 1,
    totalProjects: 5,
    planningProjects: 1,
    activeProjects: 1,
    onHoldProjects: 1,
    completedProjects: 1,
    archivedProjects: 1,
    totalProjectMemberships: 12,
    totalTasks: 5,
    todoTasks: 1,
    inProgressTasks: 1,
    blockedTasks: 1,
    completedTasks: 1,
    cancelledTasks: 1,
    overdueTasks: 2,
    taskCompletionPercentage: 25,
}

const myWorkOverview = {
    generatedAt: '2026-09-14T12:00:00Z',
    dueSoonHours: 72,
    summary: {
        totalOpen: 3,
        overdue: 1,
        dueSoon: 1,
        blocked: 1,
        inProgress: 1,
    },
    items: [
        {
            taskId: 'task-1',
            projectId: 'project-1',
            title: 'Fix blocker',
            projectName: 'Launch project',
            status: 'BLOCKED' as const,
            priority: 'URGENT' as const,
            dueAt: '2026-09-15T12:00:00Z',
            updatedAt: '2026-09-14T11:00:00Z',
            attention: 'BLOCKED' as const,
            targetUrl: '/projects/project-1?task=task-1',
        },
    ],
}

const personalWorkspaceOverview = {
    favorites: [
        {
            type: 'PROJECT' as const,
            resourceId: 'project-1',
            parentId: null,
            title: 'Launch project',
            subtitle: 'Active project',
            favoriteAt: '2026-09-14T10:00:00Z',
            lastViewedAt: '2026-09-14T10:00:00Z',
        },
    ],
    recent: [
        {
            type: 'TASK' as const,
            resourceId: 'task-2',
            parentId: 'project-2',
            title: 'Review release',
            subtitle: 'Release project',
            favoriteAt: null,
            lastViewedAt: '2026-09-14T11:00:00Z',
        },
    ],
}

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    })
}

function renderDashboard() {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <MemoryRouter>
                <ThemeProvider theme={appTheme}>
                    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
                </ThemeProvider>
            </MemoryRouter>
        )
    }

    return render(<DashboardPage />, {
        wrapper: Wrapper,
    })
}

describe('DashboardPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(myWorkApi, 'getOverview').mockResolvedValue(myWorkOverview)
        vi.spyOn(personalWorkspaceApi, 'getOverview').mockResolvedValue(personalWorkspaceOverview)
    })

    it('shows a loading state while the summary is pending', () => {
        vi.spyOn(dashboardApi, 'getSummary').mockReturnValue(new Promise(() => undefined))

        renderDashboard()

        expect(
            screen.getByRole('status', {
                name: /loading dashboard/i,
            }),
        ).toBeInTheDocument()
    })

    it('renders tenant metrics and the personal workspace overview', async () => {
        vi.spyOn(dashboardApi, 'getSummary').mockResolvedValue(dashboardSummary)

        renderDashboard()

        expect(
            await screen.findByRole('heading', {
                name: 'Example Tenant',
            }),
        ).toBeInTheDocument()

        expect(screen.getByText('25.0%')).toBeInTheDocument()
        expect(screen.getByText('12 project memberships')).toBeInTheDocument()
        expect(await screen.findByText('Fix blocker')).toBeInTheDocument()
        expect(screen.getByText('Launch project')).toBeInTheDocument()
        expect(screen.getByText('Review release')).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'Projects' })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'Users' })).toBeInTheDocument()
        expect(screen.queryByRole('link', { name: 'Organization' })).not.toBeInTheDocument()
    })

    it('keeps the tenant dashboard usable when a personal widget fails', async () => {
        vi.spyOn(dashboardApi, 'getSummary').mockResolvedValue(dashboardSummary)
        vi.mocked(myWorkApi.getOverview).mockRejectedValueOnce(new Error('My Work unavailable'))

        renderDashboard()

        expect(
            await screen.findByRole('heading', {
                name: 'Example Tenant',
            }),
        ).toBeInTheDocument()
        expect(
            await screen.findByText(/personal attention queue could not be loaded/i),
        ).toBeInTheDocument()
        expect(screen.getByText('25.0%')).toBeInTheDocument()
    })

    it('retries after a dashboard request fails', async () => {
        const user = userEvent.setup()

        vi.spyOn(dashboardApi, 'getSummary')
            .mockRejectedValueOnce(new Error('Dashboard service unavailable.'))
            .mockResolvedValueOnce(dashboardSummary)

        renderDashboard()

        expect(await screen.findByText('Dashboard service unavailable.')).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /retry/i,
            }),
        )

        expect(
            await screen.findByRole('heading', {
                name: 'Example Tenant',
            }),
        ).toBeInTheDocument()
    })
})
