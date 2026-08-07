import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import {
    afterEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import {
    WorkspaceSubscriptionAccessProvider,
} from '../context/WorkspaceSubscriptionAccessContext'
import type {
    WorkspaceSubscriptionAccess,
} from '../types/subscriptions'
import { SubscriptionEndingSoonAlert } from './SubscriptionEndingSoonAlert'

const access: WorkspaceSubscriptionAccess = {
    tenantId: 'tenant-1',
    subscriptionStatus: 'ACTIVE',
    accessLevel: 'FULL_ACCESS',
    accessReason: 'ACTIVE',
    serviceAvailable: true,
    mutationsAllowed: true,
    userCreationAllowed: true,
    projectCreationAllowed: true,
    userLimitReached: false,
    projectLimitReached: false,
    cancelAtPeriodEnd: false,
    currentPeriodEnd: '2026-09-05T12:00:00Z',
    trialEndsAt: null,
    evaluatedAt: '2026-08-07T12:00:00Z',
}

function renderAlert(
    value: WorkspaceSubscriptionAccess,
) {
    return render(
        <ThemeProvider theme={appTheme}>
            <WorkspaceSubscriptionAccessProvider
                access={value}
            >
                <SubscriptionEndingSoonAlert />
            </WorkspaceSubscriptionAccessProvider>
        </ThemeProvider>,
    )
}

describe('SubscriptionEndingSoonAlert', () => {
    afterEach(() => {
        vi.useRealTimers()
    })

    it('stays hidden for a normal active subscription', () => {
        vi.useFakeTimers()
        vi.setSystemTime(
            new Date('2026-08-07T12:00:00Z'),
        )

        renderAlert(access)

        expect(
            screen.queryByLabelText(
                'Subscription ending soon',
            ),
        ).not.toBeInTheDocument()
    })

    it('warns within seven days of scheduled cancellation', () => {
        vi.useFakeTimers()
        vi.setSystemTime(
            new Date('2026-08-07T12:00:00Z'),
        )

        renderAlert({
            ...access,
            cancelAtPeriodEnd: true,
            currentPeriodEnd: '2026-08-12T12:00:00Z',
        })

        expect(
            screen.getByText(/scheduled to end/i),
        ).toBeInTheDocument()
    })

    it('does not warn more than seven days before cancellation', () => {
        vi.useFakeTimers()
        vi.setSystemTime(
            new Date('2026-08-07T12:00:00Z'),
        )

        renderAlert({
            ...access,
            cancelAtPeriodEnd: true,
            currentPeriodEnd: '2026-08-15T12:00:01Z',
        })

        expect(
            screen.queryByLabelText(
                'Subscription ending soon',
            ),
        ).not.toBeInTheDocument()
    })

    it('warns when a trial ends within seven days', () => {
        vi.useFakeTimers()
        vi.setSystemTime(
            new Date('2026-08-07T12:00:00Z'),
        )

        renderAlert({
            ...access,
            subscriptionStatus: 'TRIALING',
            accessReason: 'TRIAL_ACTIVE',
            trialEndsAt: '2026-08-10T12:00:00Z',
        })

        expect(
            screen.getByText(/trial ends/i),
        ).toBeInTheDocument()
    })

    it('does not turn blocked state into a dashboard-wide warning', () => {
        vi.useFakeTimers()
        vi.setSystemTime(
            new Date('2026-08-07T12:00:00Z'),
        )

        renderAlert({
            ...access,
            subscriptionStatus: 'EXPIRED',
            accessLevel: 'BLOCKED',
            accessReason: 'EXPIRED',
            serviceAvailable: false,
            mutationsAllowed: false,
            userCreationAllowed: false,
            projectCreationAllowed: false,
        })

        expect(
            screen.queryByLabelText(
                'Subscription ending soon',
            ),
        ).not.toBeInTheDocument()
    })
})
