import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { systemAdminApi } from '../features/system-admin/api/systemAdminApi'
import { appTheme } from '../theme/appTheme'
import { SystemDashboardPage } from './SystemDashboardPage'

const summary = {
    totalTenants: 8,
    activeTenants: 6,
    inactiveTenants: 1,
    suspendedTenants: 1,
    totalUsers: 42,
    activeUsers: 36,
    inactiveUsers: 4,
    suspendedUsers: 2,
}

function renderDashboard() {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } },
    })
    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            </ThemeProvider>
        )
    }
    return render(<SystemDashboardPage />, { wrapper: Wrapper })
}

describe('SystemDashboardPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('renders global tenant and user metrics', async () => {
        vi.spyOn(systemAdminApi, 'getDashboardSummary').mockResolvedValue(summary)
        renderDashboard()

        expect(await screen.findByText('Total tenants')).toBeInTheDocument()
        expect(screen.getByText('8')).toBeInTheDocument()
        expect(screen.getByText('42')).toBeInTheDocument()
        expect(screen.getByText('Tenant health')).toBeInTheDocument()
        expect(screen.getByText('User account health')).toBeInTheDocument()
    })

    it('retries after the summary request fails', async () => {
        const user = userEvent.setup()
        vi.spyOn(systemAdminApi, 'getDashboardSummary')
            .mockRejectedValueOnce(new Error('Dashboard unavailable.'))
            .mockResolvedValueOnce(summary)
        renderDashboard()

        expect(await screen.findByText('Dashboard unavailable.')).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: /retry/i }))
        expect(await screen.findByText('Total tenants')).toBeInTheDocument()
    })
})
