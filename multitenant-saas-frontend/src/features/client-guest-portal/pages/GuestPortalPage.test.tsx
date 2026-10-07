import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { externalAccessApi } from '../api/externalAccessApi'
import { GuestPortalPage } from './GuestPortalPage'

const sessionToken = 'session-token'
const storageKey = 'multitenant-saas.guest-portal.session'

function renderPage() {
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

    return render(<GuestPortalPage />, { wrapper: Wrapper })
}

describe('GuestPortalPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        window.history.replaceState(null, '', '/guest')
        window.sessionStorage.clear()
        window.sessionStorage.setItem(storageKey, sessionToken)

        vi.spyOn(externalAccessApi, 'getGuestSession').mockResolvedValue({
            grantId: 'grant-1',
            guestName: 'Client Reviewer',
            guestEmail: 'client@example.com',
            capabilities: ['PROJECT_READ', 'TASK_READ', 'APPROVAL_REVIEW'],
            grantExpiresAt: '2026-10-20T10:00:00Z',
            sessionExpiresAt: '2026-10-20T10:00:00Z',
            project: {
                id: 'project-1',
                name: 'Launch Project',
                description: 'Shared launch context',
                status: 'ACTIVE',
                updatedAt: '2026-10-08T10:00:00Z',
            },
        })
        vi.spyOn(externalAccessApi, 'getGuestTasks').mockResolvedValue({ tasks: [] })
        vi.spyOn(externalAccessApi, 'getGuestApprovals').mockResolvedValue({
            reviews: [
                {
                    requestId: 'request-1',
                    requestStageId: 'stage-1',
                    taskId: '12345678-1111-2222-3333-444444444444',
                    stageName: 'Client approval',
                    createdAt: '2026-10-08T10:00:00Z',
                },
            ],
        })
    })

    it('shows assigned approval reviews and records a scoped guest decision', async () => {
        const decide = vi.spyOn(externalAccessApi, 'decideGuestApproval').mockResolvedValue({
            requestId: 'request-1',
            status: 'APPROVED',
            currentStageIndex: 0,
            completedAt: '2026-10-08T10:05:00Z',
        })

        renderPage()

        expect(await screen.findByRole('heading', { name: /approval reviews/i })).toBeVisible()
        expect(await screen.findByText('Client approval')).toBeVisible()

        fireEvent.change(screen.getByLabelText(/decision comment/i), {
            target: { value: 'Approved against release criteria' },
        })
        fireEvent.click(screen.getByRole('button', { name: /^approve$/i }))

        await waitFor(() => expect(decide).toHaveBeenCalledTimes(1))
        expect(decide).toHaveBeenCalledWith(
            sessionToken,
            'request-1',
            'APPROVE',
            'Approved against release criteria',
        )
    })
})
