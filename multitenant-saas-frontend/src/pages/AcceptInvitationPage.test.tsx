import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router'

import { invitationsApi } from '../features/invitations/api/invitationsApi'
import { appTheme } from '../theme/appTheme'
import { AcceptInvitationPage } from './AcceptInvitationPage'

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            mutations: {
                retry: false,
            },
        },
    })
}

function renderAcceptInvitationPage(initialEntry = '/accept-invitation?token=one-time-token') {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<AcceptInvitationPage />, {
        wrapper: Wrapper,
    })
}

describe('AcceptInvitationPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('rejects an incomplete invitation link', () => {
        renderAcceptInvitationPage('/accept-invitation')

        expect(screen.getByText(/invitation link is incomplete/i)).toBeInTheDocument()
        expect(
            screen.getByRole('button', {
                name: /accept invitation/i,
            }),
        ).toBeDisabled()
    })

    it('validates matching strong passwords before accepting', async () => {
        const user = userEvent.setup()
        const acceptInvitation = vi.spyOn(invitationsApi, 'acceptInvitation')

        renderAcceptInvitationPage()

        await user.type(screen.getByLabelText(/^password/i), 'Strong@123')
        await user.type(screen.getByLabelText(/confirm password/i), 'Different@123')
        await user.click(
            screen.getByRole('button', {
                name: /accept invitation/i,
            }),
        )

        expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument()
        expect(acceptInvitation).not.toHaveBeenCalled()
    })

    it('accepts the token and shows the tenant login details', async () => {
        const user = userEvent.setup()
        const acceptInvitation = vi.spyOn(invitationsApi, 'acceptInvitation').mockResolvedValue({
            user: {
                id: 'user-2',
                tenantId: 'tenant-1',
                fullName: 'Grace User',
                email: 'grace@example.com',
                role: 'TENANT_USER',
                status: 'ACTIVE',
                createdAt: '2026-08-01T10:32:00Z',
                updatedAt: '2026-08-01T10:32:00Z',
            },
            message: 'Invitation accepted successfully.',
        })

        renderAcceptInvitationPage()

        await user.type(screen.getByLabelText(/^password/i), 'Strong@123')
        await user.type(screen.getByLabelText(/confirm password/i), 'Strong@123')
        await user.click(
            screen.getByRole('button', {
                name: /accept invitation/i,
            }),
        )

        expect(
            await screen.findByRole('heading', {
                name: /welcome, grace user/i,
            }),
        ).toBeInTheDocument()
        expect(screen.getByText('tenant-1')).toBeInTheDocument()
        expect(screen.getByText('grace@example.com')).toBeInTheDocument()
        expect(acceptInvitation).toHaveBeenCalledWith({
            invitationToken: 'one-time-token',
            newPassword: 'Strong@123',
            confirmPassword: 'Strong@123',
        })
    })
})
