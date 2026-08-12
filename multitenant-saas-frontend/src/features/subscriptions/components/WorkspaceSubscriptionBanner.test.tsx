import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import type { WorkspaceSubscriptionAccess } from '../types/subscriptions'
import { WorkspaceSubscriptionBanner } from './WorkspaceSubscriptionBanner'

const activeAccess: WorkspaceSubscriptionAccess = {
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
    currentPeriodEnd: '2026-09-07T00:00:00Z',
    trialEndsAt: null,
    evaluatedAt: '2026-08-07T00:00:00Z',
}

function renderBanner(access: WorkspaceSubscriptionAccess) {
    return render(
        <ThemeProvider theme={appTheme}>
            <WorkspaceSubscriptionBanner access={access} />
        </ThemeProvider>,
    )
}

describe('WorkspaceSubscriptionBanner', () => {
    it('stays hidden for normal active access', () => {
        renderBanner(activeAccess)

        expect(screen.queryByLabelText('Workspace subscription status')).not.toBeInTheDocument()
    })

    it('warns when the workspace is in grace access', () => {
        renderBanner({
            ...activeAccess,
            subscriptionStatus: 'PAST_DUE',
            accessLevel: 'GRACE_ACCESS',
            accessReason: 'PAST_DUE_GRACE',
        })

        expect(screen.getByText(/past-due grace access/i)).toBeInTheDocument()
    })

    it('shows blocked lifecycle restrictions', () => {
        renderBanner({
            ...activeAccess,
            subscriptionStatus: 'EXPIRED',
            accessLevel: 'BLOCKED',
            accessReason: 'EXPIRED',
            serviceAvailable: false,
            mutationsAllowed: false,
            userCreationAllowed: false,
            projectCreationAllowed: false,
        })

        expect(screen.getByText(/subscription has expired/i)).toBeInTheDocument()
    })
})
