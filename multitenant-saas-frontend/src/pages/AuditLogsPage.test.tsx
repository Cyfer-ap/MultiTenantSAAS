import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    render,
    screen,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { auditLogsApi } from '../features/audit-logs/api/auditLogsApi'
import type { TenantAuditLog } from '../features/audit-logs/types/auditLogs'
import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import { appTheme } from '../theme/appTheme'
import type { PageResponse } from '../types/api'
import { AuditLogsPage } from './AuditLogsPage'

const auditLog: TenantAuditLog = {
    id: 'audit-1',
    tenantId: 'tenant-1',
    actorType: 'TENANT_USER',
    actorUserId: 'user-1',
    actorUserEmail: 'ada@example.com',
    actorSystemAdminId: null,
    actorSystemAdminEmail: null,
    targetUserId: 'user-2',
    targetUserEmail: 'grace@example.com',
    action: 'USER_ROLE_UPDATED',
    success: true,
    message: 'User role updated from User to Manager.',
    createdAt: '2026-08-01T10:30:00Z',
}

const auditLogsPage: PageResponse<TenantAuditLog> = {
    content: [auditLog],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

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
        },
    })
}

function renderAuditLogsPage() {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <AuthContext.Provider value={authContextValue}>
                        {children}
                    </AuthContext.Provider>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<AuditLogsPage />, {
        wrapper: Wrapper,
    })
}

describe('AuditLogsPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('shows a loading state while audit logs are pending', () => {
        vi.spyOn(
            auditLogsApi,
            'getAuditLogs',
        ).mockReturnValue(new Promise(() => undefined))

        renderAuditLogsPage()

        expect(
            screen.getByRole('status', {
                name: /loading audit logs/i,
            }),
        ).toBeInTheDocument()
    })

    it('renders tenant audit events', async () => {
        vi.spyOn(
            auditLogsApi,
            'getAuditLogs',
        ).mockResolvedValue(auditLogsPage)

        renderAuditLogsPage()

        expect(
            await screen.findByText('User role updated'),
        ).toBeInTheDocument()
        expect(
            screen.getByText(
                'User role updated from User to Manager.',
            ),
        ).toBeInTheDocument()
        expect(
            screen.getByText('ada@example.com'),
        ).toBeInTheDocument()
        expect(
            screen.getByText('grace@example.com'),
        ).toBeInTheDocument()
        expect(
            screen.getByText('Successful'),
        ).toBeInTheDocument()
    })

    it('submits action, outcome, and sorting parameters', async () => {
        const user = userEvent.setup()
        const getAuditLogs = vi
            .spyOn(auditLogsApi, 'getAuditLogs')
            .mockResolvedValue(auditLogsPage)

        renderAuditLogsPage()
        await screen.findByText('User role updated')

        await user.click(screen.getByLabelText(/^action$/i))
        await user.click(
            screen.getByRole('option', {
                name: 'Project created',
            }),
        )
        await user.click(screen.getByLabelText(/^outcome$/i))
        await user.click(
            screen.getByRole('option', {
                name: 'Failed',
            }),
        )
        await user.click(
            screen.getByRole('button', {
                name: /^event$/i,
            }),
        )

        expect(getAuditLogs).toHaveBeenLastCalledWith(
            'tenant-1',
            expect.objectContaining({
                action: 'PROJECT_CREATED',
                success: false,
                sortBy: 'action',
                sortDir: 'asc',
            }),
        )
    })

    it('retries after the audit-log request fails', async () => {
        const user = userEvent.setup()

        vi.spyOn(auditLogsApi, 'getAuditLogs')
            .mockRejectedValueOnce(
                new Error('Audit service unavailable.'),
            )
            .mockResolvedValueOnce(auditLogsPage)

        renderAuditLogsPage()

        expect(
            await screen.findByText(
                'Audit service unavailable.',
            ),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /retry/i,
            }),
        )

        expect(
            await screen.findByText('User role updated'),
        ).toBeInTheDocument()
    })
})
