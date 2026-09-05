import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiClientError } from '../api/apiError'
import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import {
    useBillingCheckoutConfiguration,
    useCancelBillingSubscription,
    useCreateBillingCheckout,
    useWorkspaceSubscription,
    useWorkspaceSubscriptionEntitlements,
} from '../features/subscriptions/hooks/useWorkspaceSubscription'
import { appTheme } from '../theme/appTheme'
import { TenantSubscriptionPage } from './TenantSubscriptionPage'

vi.mock('../features/subscriptions/hooks/useWorkspaceSubscription', () => ({
    useBillingCheckoutConfiguration: vi.fn(),
    useCancelBillingSubscription: vi.fn(),
    useCreateBillingCheckout: vi.fn(),
    useWorkspaceSubscription: vi.fn(),
    useWorkspaceSubscriptionEntitlements: vi.fn(),
}))

const createCheckout = vi.fn()
const cancelSubscription = vi.fn()

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

function noSubscriptionQuery() {
    return {
        data: undefined,
        error: new ApiClientError({
            message: 'Subscription not found.',
            status: 404,
        }),
        isError: true,
        isFetching: false,
        isPending: false,
        refetch: vi.fn(),
    } as never
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
        window.history.replaceState(null, '', '/subscription')
        createCheckout.mockReset()
        cancelSubscription.mockReset()
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
        vi.mocked(useCancelBillingSubscription).mockReturnValue({
            data: undefined,
            error: null,
            isError: false,
            isPending: false,
            isSuccess: false,
            mutate: cancelSubscription,
        } as never)
    })

    it('renders readable current-plan, billing, period, and limit details', () => {
        renderPage()

        expect(screen.getByRole('heading', { name: 'Growth' })).toBeInTheDocument()
        expect(screen.getByText('GROWTH')).toBeInTheDocument()
        expect(screen.getByText('2 / 25')).toBeInTheDocument()
        expect(screen.getByText('7 / 100')).toBeInTheDocument()
        expect(screen.queryByText('subscription-1')).not.toBeInTheDocument()
        expect(screen.queryByText('plan-1')).not.toBeInTheDocument()
    })

    it('does not enable checkout discovery for an active subscription', () => {
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

        expect(useBillingCheckoutConfiguration).toHaveBeenCalledWith('tenant-1', false)
        expect(screen.queryByRole('button', { name: 'Choose Growth' })).not.toBeInTheDocument()
        expect(
            screen.queryByRole('button', { name: 'Pay securely with Razorpay' }),
        ).not.toBeInTheDocument()
    })

    it('shows a purchase-oriented empty state when no subscription is assigned', () => {
        vi.mocked(useWorkspaceSubscription).mockReturnValue(noSubscriptionQuery())

        renderPage()

        expect(screen.getByText('Start a workspace subscription')).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: 'Choose your plan' })).toBeInTheDocument()
    })

    it('reveals payment methods only after a plan is selected', async () => {
        const user = userEvent.setup()
        vi.mocked(useWorkspaceSubscription).mockReturnValue(noSubscriptionQuery())
        vi.mocked(useBillingCheckoutConfiguration).mockReturnValue({
            data: {
                plans: [subscription.plan],
                providers: ['RAZORPAY', 'STRIPE'],
            },
            error: null,
            isError: false,
            isPending: false,
        } as never)

        renderPage()

        expect(screen.queryByText('Select a payment method')).not.toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: 'Choose Growth' }))

        expect(screen.getByRole('heading', { name: 'Select a payment method' })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Pay securely with Stripe' })).toBeInTheDocument()
        expect(
            screen.getByRole('button', { name: 'Pay securely with Razorpay' }),
        ).toBeInTheDocument()
    })

    it('starts Razorpay checkout for the selected paid plan', async () => {
        const user = userEvent.setup()
        vi.mocked(useWorkspaceSubscription).mockReturnValue(noSubscriptionQuery())
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
        await user.click(screen.getByRole('button', { name: 'Choose Growth' }))
        await user.click(screen.getByRole('button', { name: 'Pay securely with Razorpay' }))

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

    it('starts Stripe checkout while Razorpay remains an available alternative', async () => {
        const user = userEvent.setup()
        vi.mocked(useWorkspaceSubscription).mockReturnValue(noSubscriptionQuery())
        vi.mocked(useBillingCheckoutConfiguration).mockReturnValue({
            data: {
                plans: [subscription.plan],
                providers: ['RAZORPAY', 'STRIPE'],
            },
            error: null,
            isError: false,
            isPending: false,
        } as never)

        renderPage()
        await user.click(screen.getByRole('button', { name: 'Choose Growth' }))

        expect(
            screen.getByRole('button', { name: 'Pay securely with Razorpay' }),
        ).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: 'Pay securely with Stripe' }))

        expect(createCheckout).toHaveBeenCalledWith(
            {
                tenantId: 'tenant-1',
                input: {
                    planCode: 'GROWTH',
                    provider: 'STRIPE',
                },
            },
            expect.objectContaining({
                onSuccess: expect.any(Function),
            }),
        )
    })

    it('explains successful and cancelled hosted-checkout returns', () => {
        window.history.replaceState(null, '', '/subscription?checkout=success')
        const { unmount } = renderPage()
        expect(screen.getByText(/Checkout completed/)).toBeInTheDocument()

        unmount()
        window.history.replaceState(null, '', '/subscription?checkout=cancelled')
        renderPage()
        expect(screen.getByText(/Checkout was cancelled/)).toBeInTheDocument()
    })

    it('requires confirmation before requesting provider-backed cancellation', async () => {
        const user = userEvent.setup()
        renderPage()

        await user.click(screen.getByRole('button', { name: 'Cancel subscription' }))
        expect(screen.getByRole('dialog', { name: 'Cancel subscription?' })).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: 'Confirm cancellation' }))

        expect(cancelSubscription).toHaveBeenCalledWith(
            { tenantId: 'tenant-1' },
            expect.objectContaining({
                onSuccess: expect.any(Function),
            }),
        )
    })
})
