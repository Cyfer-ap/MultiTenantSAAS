import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import { authorizationApi } from '../features/authorization/api/authorizationApi'
import { createTenantAuthorizationContext } from '../features/authorization/test/authorizationTestData'
import { authorizationPermissionCodes } from '../features/authorization/types/authorization'
import { invitationsApi } from '../features/invitations/api/invitationsApi'
import type { CreatedInvitation, TenantInvitation } from '../features/invitations/types/invitations'
import { appTheme } from '../theme/appTheme'
import type { PageResponse } from '../types/api'
import { InvitationsPage } from './InvitationsPage'

const invitation: TenantInvitation = {
    invitationId: 'invitation-1',
    tenantId: 'tenant-1',
    fullName: 'Grace User',
    email: 'grace@example.com',
    role: 'TENANT_USER',
    status: 'PENDING',
    active: true,
    expired: false,
    expiresAt: '2026-08-03T10:30:00Z',
    createdAt: '2026-08-01T10:30:00Z',
    acceptedAt: null,
    revokedAt: null,
    invitedByUserId: 'user-1',
    invitedByUserEmail: 'ada@example.com',
    invitedBySystemAdminId: null,
    invitedBySystemAdminEmail: null,
}

const invitationsPage: PageResponse<TenantInvitation> = {
    content: [invitation],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

const invitationManagementAuthorization = createTenantAuthorizationContext({
    permissionCodes: [
        authorizationPermissionCodes.USER_READ,
        authorizationPermissionCodes.USER_CREATE,
    ],
})

const invitationReadOnlyAuthorization = createTenantAuthorizationContext({
    permissionCodes: [authorizationPermissionCodes.USER_READ],
})

const authContextValue: AuthContextValue = {
    status: 'authenticated',
    session: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
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

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
            mutations: {
                retry: false,
            },
        },
    })
}

function renderInvitationsPage(contextValue: AuthContextValue = authContextValue) {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<InvitationsPage />, {
        wrapper: Wrapper,
    })
}

describe('InvitationsPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()

        vi.spyOn(authorizationApi, 'getCurrentAuthorizationContext').mockResolvedValue(
            invitationManagementAuthorization,
        )
    })

    it('shows a loading state while invitations are pending', () => {
        vi.spyOn(invitationsApi, 'getInvitations').mockReturnValue(new Promise(() => undefined))

        renderInvitationsPage()

        expect(
            screen.getByRole('status', {
                name: /loading invitations/i,
            }),
        ).toBeInTheDocument()
    })

    it('renders tenant invitations returned by the API', async () => {
        vi.spyOn(invitationsApi, 'getInvitations').mockResolvedValue(invitationsPage)

        renderInvitationsPage()

        expect(await screen.findByText('Grace User')).toBeInTheDocument()
        expect(screen.getByText('grace@example.com')).toBeInTheDocument()
        expect(screen.getByText('ada@example.com')).toBeInTheDocument()
        expect(screen.getByText('Pending')).toBeInTheDocument()
    })

    it('submits search, role, status, and sorting parameters', async () => {
        const getInvitations = vi
            .spyOn(invitationsApi, 'getInvitations')
            .mockResolvedValue(invitationsPage)

        renderInvitationsPage()
        await screen.findByText('Grace User')

        fireEvent.change(screen.getByLabelText(/search invitations/i), {
            target: {
                value: '  grace@example.com  ',
            },
        })
        fireEvent.mouseDown(screen.getByLabelText(/^status$/i))
        fireEvent.click(screen.getByRole('option', { name: 'Pending' }))
        fireEvent.mouseDown(screen.getByLabelText(/^role$/i))
        fireEvent.click(screen.getByRole('option', { name: 'User' }))
        fireEvent.click(screen.getByRole('button', { name: /^search$/i }))
        fireEvent.click(screen.getByRole('button', { name: /^invitee$/i }))

        await waitFor(() => {
            expect(getInvitations).toHaveBeenLastCalledWith(
                'tenant-1',
                expect.objectContaining({
                    page: 0,
                    search: 'grace@example.com',
                    status: 'PENDING',
                    role: 'TENANT_USER',
                    sortBy: 'fullName',
                    sortDir: 'asc',
                }),
            )
        })
    }, 10_000)

    it('hides invitation mutations without user.create permission', async () => {
        vi.spyOn(invitationsApi, 'getInvitations').mockResolvedValue(invitationsPage)
        vi.mocked(authorizationApi.getCurrentAuthorizationContext).mockResolvedValue(
            invitationReadOnlyAuthorization,
        )

        renderInvitationsPage()

        await screen.findByText('Grace User')

        expect(
            screen.queryByRole('button', {
                name: /invite user/i,
            }),
        ).not.toBeInTheDocument()

        expect(
            screen.queryByRole('button', {
                name: /revoke invitation for grace user/i,
            }),
        ).not.toBeInTheDocument()
    })

    it('uses V2 invitation permission rather than the legacy role', async () => {
        vi.spyOn(invitationsApi, 'getInvitations').mockResolvedValue(invitationsPage)

        renderInvitationsPage({
            ...authContextValue,
            session: {
                ...authContextValue.session!,
                role: 'TENANT_USER',
            },
        })

        expect(
            await screen.findByRole('button', {
                name: /invite user/i,
            }),
        ).toBeInTheDocument()

        expect(
            await screen.findByRole('button', {
                name: /revoke invitation for grace user/i,
            }),
        ).toBeInTheDocument()
    })

    it('creates a normalized invitation and exposes its acceptance link', async () => {
        const user = userEvent.setup()
        const createdInvitation: CreatedInvitation = {
            invitationId: 'invitation-2',
            tenantId: 'tenant-1',
            fullName: 'Lin User',
            email: 'lin@example.com',
            role: 'TENANT_USER',
            status: 'PENDING',
            expiresAt: '2026-08-03T10:30:00Z',
            devInvitationToken: 'one-time-token',
            message: 'Invitation generated successfully.',
        }

        vi.spyOn(invitationsApi, 'getInvitations').mockResolvedValue(invitationsPage)
        const createInvitation = vi
            .spyOn(invitationsApi, 'createInvitation')
            .mockResolvedValue(createdInvitation)

        renderInvitationsPage()
        await screen.findByText('Grace User')
        await user.click(
            await screen.findByRole('button', {
                name: /invite user/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /invite tenant user/i,
        })
        await user.type(within(dialog).getByLabelText(/full name/i), '  Lin User  ')
        await user.type(within(dialog).getByLabelText(/^email/i), '  LIN@EXAMPLE.COM  ')
        await user.click(
            within(dialog).getByRole('button', {
                name: /create invitation/i,
            }),
        )

        await waitFor(() => {
            expect(createInvitation).toHaveBeenCalledWith('tenant-1', {
                fullName: 'Lin User',
                email: 'lin@example.com',
                role: 'TENANT_USER',
            })
        })

        expect(
            await screen.findByRole('dialog', {
                name: /invitation ready/i,
            }),
        ).toBeInTheDocument()
        const acceptanceLink = screen.getByLabelText(/invitation link/i) as HTMLInputElement

        expect(acceptanceLink.value).toMatch(/\/accept-invitation\?token=one-time-token$/)
    })

    it('revokes a pending invitation', async () => {
        const user = userEvent.setup()

        vi.spyOn(invitationsApi, 'getInvitations').mockResolvedValue(invitationsPage)
        const revokeInvitation = vi.spyOn(invitationsApi, 'revokeInvitation').mockResolvedValue({
            ...invitation,
            status: 'REVOKED',
            active: false,
        })

        renderInvitationsPage()
        await screen.findByText('Grace User')
        await user.click(
            await screen.findByRole('button', {
                name: /revoke invitation for grace user/i,
            }),
        )
        await user.click(
            within(
                screen.getByRole('dialog', {
                    name: /revoke invitation/i,
                }),
            ).getByRole('button', {
                name: /^revoke invitation$/i,
            }),
        )

        await waitFor(() => {
            expect(revokeInvitation).toHaveBeenCalledWith('tenant-1', 'invitation-1')
        })
        expect(
            await screen.findByText(/invitation for grace@example.com was revoked/i),
        ).toBeInTheDocument()
    })

    it('retries after the invitation request fails', async () => {
        const user = userEvent.setup()

        vi.spyOn(invitationsApi, 'getInvitations')
            .mockRejectedValueOnce(new Error('Invitation service unavailable.'))
            .mockResolvedValueOnce(invitationsPage)

        renderInvitationsPage()

        expect(await screen.findByText('Invitation service unavailable.')).toBeInTheDocument()
        await user.click(screen.getByRole('button', { name: /retry/i }))
        expect(await screen.findByText('Grace User')).toBeInTheDocument()
    })
})
