import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { notificationsApi } from '../api/notificationsApi'
import type { NotificationPreference } from '../types/notifications'
import { NotificationPreferencesCard } from './NotificationPreferencesCard'

const preferences: NotificationPreference[] = [
    {
        type: 'TASK_ASSIGNED',
        inAppEnabled: true,
        emailEnabled: true,
        emailConfigurable: true,
    },
    {
        type: 'SECURITY_ALERT',
        inAppEnabled: true,
        emailEnabled: true,
        emailConfigurable: false,
    },
]

function renderCard() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<NotificationPreferencesCard tenantId="tenant-1" />, { wrapper: Wrapper })
}

describe('NotificationPreferencesCard', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(notificationsApi, 'getPreferences').mockResolvedValue(preferences)
    })

    it('keeps in-app notifications mandatory and allows optional email opt-out', async () => {
        const user = userEvent.setup()
        const update = vi.spyOn(notificationsApi, 'updatePreference').mockResolvedValue({
            ...preferences[0],
            emailEnabled: false,
        })

        renderCard()

        expect(await screen.findByText('Task assignments')).toBeInTheDocument()
        expect(screen.getByText('In-app · always on')).toBeInTheDocument()

        const taskEmail = screen.getByRole('checkbox', {
            name: 'Task assignments email notifications',
        })
        expect(taskEmail).toBeChecked()

        await user.click(taskEmail)

        await waitFor(() => {
            expect(update).toHaveBeenCalledWith('tenant-1', 'TASK_ASSIGNED', {
                emailEnabled: false,
            })
        })
    })

    it('does not allow security alert email delivery to be disabled', async () => {
        renderCard()

        const securityEmail = await screen.findByRole('checkbox', {
            name: 'Security alerts email notifications',
        })
        expect(securityEmail).toBeChecked()
        expect(securityEmail).toBeDisabled()
        expect(screen.getByText('Required')).toBeInTheDocument()
    })
})
