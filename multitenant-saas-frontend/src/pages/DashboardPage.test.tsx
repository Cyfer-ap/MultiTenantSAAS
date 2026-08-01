import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    render,
    screen,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { dashboardApi } from '../features/dashboard/api/dashboardApi'
import type { TenantDashboardSummary } from '../features/dashboard/types/dashboard'
import { appTheme } from '../theme/appTheme'
import { DashboardPage } from './DashboardPage'

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
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    {children}
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<DashboardPage />, {
        wrapper: Wrapper,
    })
}

describe('DashboardPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('shows a loading state while the summary is pending', () => {
        vi.spyOn(
            dashboardApi,
            'getSummary',
        ).mockReturnValue(new Promise(() => undefined))

        renderDashboard()

        expect(
            screen.getByRole('status', {
                name: /loading dashboard/i,
            }),
        ).toBeInTheDocument()
    })

    it('renders the tenant dashboard metrics', async () => {
        vi.spyOn(
            dashboardApi,
            'getSummary',
        ).mockResolvedValue(dashboardSummary)

        renderDashboard()

        expect(
            await screen.findByRole('heading', {
                name: 'Example Tenant',
            }),
        ).toBeInTheDocument()

        expect(
            screen.getByText('8'),
        ).toBeInTheDocument()

        expect(
            screen.getByText('25.0%'),
        ).toBeInTheDocument()

        expect(
            screen.getByText('12 project memberships'),
        ).toBeInTheDocument()
    })

    it('retries after a dashboard request fails', async () => {
        const user = userEvent.setup()

        vi.spyOn(dashboardApi, 'getSummary')
            .mockRejectedValueOnce(
                new Error('Dashboard service unavailable.'),
            )
            .mockResolvedValueOnce(dashboardSummary)

        renderDashboard()

        expect(
            await screen.findByText(
                'Dashboard service unavailable.',
            ),
        ).toBeInTheDocument()

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
