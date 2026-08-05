import { ThemeProvider } from '@mui/material'
import {
    render,
    screen,
} from '@testing-library/react'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { ApiClientError } from '../api/apiError'
import { AuthContext } from '../features/auth/context/AuthContext'
import type {
    AuthContextValue,
} from '../features/auth/context/AuthContext'
import {
    useWorkspaceSubscription,
} from '../features/subscriptions/hooks/useWorkspaceSubscription'
import { appTheme } from '../theme/appTheme'
import { TenantSubscriptionPage } from './TenantSubscriptionPage'

vi.mock(
    '../features/subscriptions/hooks/useWorkspaceSubscription',
    () => ({
        useWorkspaceSubscription: vi.fn(),
    }),
)

const authContextValue: AuthContextValue = {
    status: 'authenticated',
    session: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        tokenType: 'Bearer',
        accessTokenExpiresAt:
            Date.now() + 60_000,
        tenantId: 'tenant-1',
        userId: 'user-1',
        fullName: 'Ada Admin',
        email: 'ada@example.com',
        role: 'TENANT_ADMIN',
    },
    login: vi.fn(),
    logout: vi.fn(),
}

const subscription = {
    id: 'subscription-1',
    tenantId: 'tenant-1',
    tenantName: 'Research Lab',
    plan: {
        id: 'plan-1',
        code: 'GROWTH',
        name: 'Growth',
        description:
            'For growing product teams.',
        billingInterval: 'MONTHLY' as const,
        price: 49,
        currency: 'USD',
        maxUsers: 25,
        maxProjects: 100,
        maxStorageMb: 10240,
        status: 'ACTIVE' as const,
        createdAt: '2026-08-05T12:00:00Z',
        updatedAt: '2026-08-05T12:00:00Z',
    },
    status: 'ACTIVE' as const,
    startedAt: '2026-08-05T12:00:00Z',
    currentPeriodStart:
        '2026-08-05T12:00:00Z',
    currentPeriodEnd:
        '2026-09-05T12:00:00Z',
    trialEndsAt: null,
    cancelAtPeriodEnd: false,
    cancelledAt: null,
    createdAt: '2026-08-05T12:00:00Z',
    updatedAt: '2026-08-05T12:00:00Z',
}

function renderPage() {
    return render(
        <ThemeProvider theme={appTheme}>
            <AuthContext.Provider
                value={authContextValue}
            >
                <TenantSubscriptionPage />
            </AuthContext.Provider>
        </ThemeProvider>,
    )
}

describe('TenantSubscriptionPage', () => {
    beforeEach(() => {
        vi.mocked(
            useWorkspaceSubscription,
        ).mockReturnValue({
            data: subscription,
            error: null,
            isError: false,
            isFetching: false,
            isPending: false,
            refetch: vi.fn(),
        } as never)
    })

    it('renders readable plan, billing, period, and limit details', () => {
        renderPage()

        expect(
            screen.getByRole('heading', {
                name: 'Growth',
            }),
        ).toBeInTheDocument()
        expect(
            screen.getByText('GROWTH'),
        ).toBeInTheDocument()
        expect(
            screen.getByText('25'),
        ).toBeInTheDocument()
        expect(
            screen.getByText('100'),
        ).toBeInTheDocument()
        expect(
            screen.queryByText(
                'subscription-1',
            ),
        ).not.toBeInTheDocument()
        expect(
            screen.queryByText('plan-1'),
        ).not.toBeInTheDocument()
    })

    it('shows a clear empty state when no plan is assigned', () => {
        vi.mocked(
            useWorkspaceSubscription,
        ).mockReturnValue({
            data: undefined,
            error: new ApiClientError({
                message:
                    'Subscription not found.',
                status: 404,
            }),
            isError: true,
            isFetching: false,
            isPending: false,
            refetch: vi.fn(),
        } as never)

        renderPage()

        expect(
            screen.getByText(
                'No subscription assigned',
            ),
        ).toBeInTheDocument()
    })
})
