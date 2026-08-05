import { ThemeProvider } from '@mui/material'
import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useSubscriptionPlans, useTenantSubscription, useUpdateSubscriptionPlanStatus } from '../features/subscriptions/hooks/useSystemSubscriptions'
import { useSystemTenants } from '../features/system-admin/hooks/useSystemTenants'
import { appTheme } from '../theme/appTheme'
import { SystemSubscriptionsPage } from './SystemSubscriptionsPage'

vi.mock('../features/subscriptions/hooks/useSystemSubscriptions', () => ({
    useSubscriptionPlans: vi.fn(),
    useTenantSubscription: vi.fn(),
    useUpdateSubscriptionPlanStatus: vi.fn(),
}))
vi.mock('../features/system-admin/hooks/useSystemTenants', () => ({
    useSystemTenants: vi.fn(),
}))

const plan = {
    id: 'plan-1',
    code: 'GROWTH',
    name: 'Growth',
    description: null,
    billingInterval: 'MONTHLY' as const,
    price: 49,
    currency: 'USD',
    maxUsers: 25,
    maxProjects: 100,
    maxStorageMb: 10240,
    status: 'ACTIVE' as const,
    createdAt: '2026-08-05T12:00:00Z',
    updatedAt: '2026-08-05T12:00:00Z',
}

function renderPage() {
    return render(
        <ThemeProvider theme={appTheme}>
            <SystemSubscriptionsPage />
        </ThemeProvider>,
    )
}

describe('SystemSubscriptionsPage', () => {
    beforeEach(() => {
        vi.mocked(useSubscriptionPlans).mockReturnValue({
            data: [plan],
            isError: false,
            isFetching: false,
            isLoading: false,
            refetch: vi.fn(),
            error: null,
        } as never)
        vi.mocked(useSystemTenants).mockReturnValue({
            data: { content: [], page: 0, size: 25, totalElements: 0, totalPages: 0, first: true, last: true },
            isError: false,
            isFetching: false,
            error: null,
        } as never)
        vi.mocked(useTenantSubscription).mockReturnValue({
            data: undefined,
            isError: false,
            isLoading: false,
            error: null,
            refetch: vi.fn(),
        } as never)
        vi.mocked(useUpdateSubscriptionPlanStatus).mockReturnValue({
            isError: false,
            isPending: false,
            error: null,
            mutateAsync: vi.fn(),
        } as never)
    })

    it('renders human-readable plan information without exposing identifiers', () => {
        renderPage()
        expect(screen.getByText('Growth')).toBeInTheDocument()
        expect(screen.getByText('GROWTH')).toBeInTheDocument()
        expect(screen.queryByText('plan-1')).not.toBeInTheDocument()
    })

    it('uses tenant search instead of a raw UUID field', () => {
        renderPage()
        fireEvent.click(screen.getByRole('tab', { name: 'Tenant subscriptions' }))
        expect(screen.getByLabelText('Find tenant')).toBeInTheDocument()
        expect(screen.queryByLabelText(/uuid/i)).not.toBeInTheDocument()
    })
})
