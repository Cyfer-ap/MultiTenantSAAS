import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiClientError } from '../api/apiError'
import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import {
    useBillingCheckoutConfiguration,
    useCreateBillingCheckout,
    useWorkspaceSubscription,
    useWorkspaceSubscriptionEntitlements,
} from '../features/subscriptions/hooks/useWorkspaceSubscription'
import { appTheme } from '../theme/appTheme'
import { TenantSubscriptionPage } from './TenantSubscriptionPage'

vi.mock('../features/subscriptions/hooks/useWorkspaceSubscription', () => ({
    useBillingCheckoutConfiguration: vi.fn(),
    useCreateBillingCheckout: vi.fn(),
    useWorkspaceSubscription: vi.fn(),
    useWorkspaceSubscriptionEntitlements: vi.fn(),
}))

const createCheckout = vi.fn()

const authContextValue: AuthContextValue = {
    status: 'authenticated',
    session: {
        accessToken: 'access-token',
        csrfToken: 'csrf-token',
        persistentSession: true,
        tokenType: 'Bearer',
        accessTokenExpiresAt: Date.now() + 60_000,
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
        description: 'For growing product teams.',
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
    currentPeriodStart: '2026-08-05T12:00:00Z',
    currentPeriodEnd: '2026-09-05T12:00:00Z',
    trialEndsAt: null,
    cancelAtPeriodEnd: false,
    cancelledAt: null,
    createdAt: '2026-08-05T12:00:00Z',
    updatedAt: '2026-08-05T12:00:00Z',
}

const entitlements = {
    tenantId: 'tenant-1',
    subscriptionId: 'subscription-1',
    planId: 'plan-1',
    planCode: 'GROWTH',
    planName: 'Growth',
    subscriptionStatus: 'ACTIVE' as const,
    accessLevel: 'FULL_ACCESS' as const,
    accessReason: 'ACTIVE' as const,
    serviceAvailable: true,
    mutationsAllowed: true,
    cancelAtPeriodEnd: false,
    currentPeriodEnd: '2026-09-05T12:00:00Z',
    trialEndsAt: null,
    evaluatedAt: '2026-08-07T12:00:00Z',
    users: {
        used: 2,
        limit: 25,
        remaining: 23,
        unlimited: false,
        limitReached: false,
        overLimit: false,
        creationAllowed: true,
    },
    projects: {
        used: 7,
        limit: 100,
        remaining: 93,
        unlimited: false,
        limitReached: false,
        overLimit: false,
        creationAllowed: true,
    },
}

function renderPage() {
    return render(
        <ThemeProvider theme={appTheme}>
            <AuthContext.Provider value={authContextValue}>
                <TenantSubscriptionPage />
            </AuthContext.Provider>
        </ThemeProvider>,
    )
}

describe('TenantSubscriptionPage', () => {
    beforeEach(() => {
        createCheckout.mockReset()
        vi.mocked(useWorkspaceSubscription).mockReturnValue({
            data: subscription,
            error: null,
            isError: false,
            isFetching: false,
            isPending: false,
            refetch: vi.fn(),
        } as never)
        vi.mocked(useWorkspaceSubscriptionEntitlements).mockReturnValue({
            data: entitlements,
            error: null,
            isError: false,
            isFetching: false,
            isPending: false,
            refetch: vi.fn(),
        } as never)
        vi.mocked(useBillingCheckoutConfiguration).mockReturnValue({
            data: {
                plans: [],
                providers: [],
            },
            error: null,
            isError: false,
            isPending: false,
        } as never)
        vi.mocked(useCreateBillingCheckout).mockReturnValue({
            error: null,
            isError: false,
            isPending: false,
            mutate: createCheckout,
        } as never)
    })

    it('renders readable plan, billing, period, and limit details', () => {
        renderPage()

        expect(
            screen.getByRole('heading', {
                name: 'Growth',
            }),
        ).toBeInTheDocument()
        expect(screen.getByText('GROWTH')).toBeInTheDocument()
        expect(screen.getByText('2 / 25')).toBeInTheDocument()
        expect(screen.getByText('7 / 100')).toBeInTheDocument()
        expect(screen.queryByText('subscription-1')).not.toBeInTheDocument()
        expect(screen.queryByText('plan-1')).not.toBeInTheDocument()
    })

    it('shows a clear empty state when no plan is assigned', () => {
        vi.mocked(useWorkspaceSubscription).mockReturnValue({
            data: undefined,
            error: new ApiClientError({
                message: 'Subscription not found.',
                status: 404,
            }),
            isError: true,
            isFetching: false,
            isPending: false,
            refetch: vi.fn(),
        } as never)

        renderPage()

        expect(screen.getByText('No subscription assigned')).toBeInTheDocument()
    })

    it('starts Razorpay checkout for an available paid plan', async () => {
        const user = userEvent.setup()
        vi.mocked(useWorkspaceSubscription).mockReturnValue({
            data: undefined,
            error: new ApiClientError({
                message: 'Subscription not found.',
                status: 404,
            }),
            isError: true,
            isFetching: false,
            isPending: false,
            refetch: vi.fn(),
        } as never)
        vi.mocked(useBillingCheckoutConfiguration).mockReturnValue({
            data: {
                plans: [subscription.plan],
                providers: ['RAZORPAY'],
            },
            error: null,
            isError: false,
            isPending: false,
        } as never)

        renderPage()
        await user.click(screen.getByRole('button', { name: 'Continue with Razorpay' }))

        expect(createCheckout).toHaveBeenCalledWith(
            {
                tenantId: 'tenant-1',
                input: {
                    planCode: 'GROWTH',
                    provider: 'RAZORPAY',
                },
            },
            expect.objectContaining({
                onSuccess: expect.any(Function),
            }),
        )
    })
})
