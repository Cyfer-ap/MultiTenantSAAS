import { ThemeProvider } from '@mui/material'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import type { TenantSubscriptionHistoryPage } from '../types/subscriptionHistory'
import { SubscriptionHistoryPanel } from './SubscriptionHistoryPanel'

const history: TenantSubscriptionHistoryPage = {
    content: [
        {
            id: 'history-1',
            subscriptionId: 'subscription-1',
            tenantId: 'tenant-1',
            tenantName: 'Research Lab',
            planId: 'plan-1',
            planCode: 'GROWTH',
            planName: 'Growth',
            planDescription: null,
            billingInterval: 'MONTHLY',
            price: 49,
            currency: 'USD',
            maxUsers: 25,
            maxProjects: 100,
            maxStorageMb: 10240,
            status: 'ACTIVE',
            startedAt: '2026-08-05T12:00:00Z',
            currentPeriodStart: '2026-09-05T12:00:00Z',
            currentPeriodEnd: '2026-10-05T12:00:00Z',
            trialEndsAt: null,
            cancelAtPeriodEnd: false,
            cancelledAt: null,
            billingProvider: 'STRIPE',
            providerSubscriptionId: 'sub_provider_secret_reference',
            providerEventCreatedAt: '2026-09-05T12:00:00Z',
            eventType: 'PROVIDER_SYNCHRONIZED',
            recordedAt: '2026-09-05T12:00:01Z',
        },
    ],
    page: 0,
    size: 10,
    totalElements: 11,
    totalPages: 2,
    first: true,
    last: false,
}

function renderPanel(showProviderReference = false) {
    const onPageChange = vi.fn()
    render(
        <ThemeProvider theme={appTheme}>
            <SubscriptionHistoryPanel
                data={history}
                onPageChange={onPageChange}
                onRefresh={vi.fn()}
                page={0}
                showProviderReference={showProviderReference}
            />
        </ThemeProvider>,
    )
    return onPageChange
}

describe('SubscriptionHistoryPanel', () => {
    it('renders readable immutable history without exposing provider references to tenants', () => {
        renderPanel()

        expect(screen.getByText('Provider update')).toBeInTheDocument()
        expect(screen.getByText('Growth')).toBeInTheDocument()
        expect(screen.getByText('Stripe')).toBeInTheDocument()
        expect(screen.queryByText('sub_provider_secret_reference')).not.toBeInTheDocument()
        expect(screen.queryByText('subscription-1')).not.toBeInTheDocument()
    })

    it('shows provider references to system admins and paginates forward', () => {
        const onPageChange = renderPanel(true)

        expect(screen.getByText('sub_provider_secret_reference')).toBeInTheDocument()
        fireEvent.click(screen.getByRole('button', { name: 'Next' }))
        expect(onPageChange).toHaveBeenCalledWith(1)
    })
})
