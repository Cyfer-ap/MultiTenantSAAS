import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    fireEvent,
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

import { systemAdminApi } from '../features/system-admin/api/systemAdminApi'
import type { PlatformAuditLog } from '../features/system-admin/types/systemAdmin'
import { appTheme } from '../theme/appTheme'
import type { PageResponse } from '../types/api'
import { PlatformAuditLogsPage } from './PlatformAuditLogsPage'

const auditLog: PlatformAuditLog = {
    id: 'audit-1',
    actorSystemAdminId: 'admin-1',
    actorSystemAdminEmail: 'owner@example.com',
    targetSystemAdminId: 'admin-2',
    targetSystemAdminEmail: 'security@example.com',
    action: 'SYSTEM_ADMIN_STATUS_UPDATED',
    success: true,
    message: 'System admin status updated for security@example.com from ACTIVE to SUSPENDED',
    createdAt: '2026-08-02T10:00:00Z',
}

const auditPage: PageResponse<PlatformAuditLog> = {
    content: [auditLog],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

function renderAuditLogs() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    {children}
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<PlatformAuditLogsPage />, { wrapper: Wrapper })
}

describe('PlatformAuditLogsPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(systemAdminApi, 'getPlatformAuditLogs')
            .mockResolvedValue(auditPage)
    })

    it('renders actor, target, event, outcome, and message', async () => {
        renderAuditLogs()

        expect(await screen.findByText('System admin status updated')).toBeInTheDocument()
        expect(screen.getByText('owner@example.com')).toBeInTheDocument()
        expect(screen.getByText('security@example.com')).toBeInTheDocument()
        expect(screen.getByText(/from ACTIVE to SUSPENDED/i)).toBeInTheDocument()
        expect(screen.getByText('Successful')).toBeInTheDocument()
    })

    it('submits search, action, outcome, and sorting parameters', async () => {
        const user = userEvent.setup()
        const getLogs = vi.mocked(systemAdminApi.getPlatformAuditLogs)
        renderAuditLogs()
        await screen.findByText('System admin status updated')

        fireEvent.change(screen.getByLabelText(/search platform events/i), {
            target: { value: '  security@example.com  ' },
        })
        await user.click(screen.getByLabelText(/^action$/i))
        await user.click(screen.getByRole('option', {
            name: 'System admin login unlocked',
        }))
        await user.click(screen.getByLabelText(/^outcome$/i))
        await user.click(screen.getByRole('option', { name: 'Failed' }))
        await user.click(screen.getByRole('button', { name: /^search$/i }))
        await user.click(screen.getByRole('button', { name: /^event$/i }))

        expect(getLogs).toHaveBeenLastCalledWith(
            expect.objectContaining({
                action: 'SYSTEM_ADMIN_LOGIN_UNLOCKED',
                page: 0,
                search: 'security@example.com',
                sortBy: 'action',
                sortDir: 'asc',
                success: false,
            }),
        )
    })
})
