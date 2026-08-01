import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    fireEvent,
    render,
    screen,
    within,
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
import type { SystemTenant } from '../features/system-admin/types/systemAdmin'
import { appTheme } from '../theme/appTheme'
import type { PageResponse } from '../types/api'
import { SystemTenantsPage } from './SystemTenantsPage'

const tenant: SystemTenant = {
    id: 'tenant-1',
    name: 'Research Lab',
    slug: 'research-lab',
    status: 'ACTIVE',
    createdAt: '2026-08-01T10:00:00Z',
    updatedAt: '2026-08-02T10:00:00Z',
}

const tenantPage: PageResponse<SystemTenant> = {
    content: [tenant],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

function renderTenants() {
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

    return render(<SystemTenantsPage />, { wrapper: Wrapper })
}

describe('SystemTenantsPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(systemAdminApi, 'getTenants').mockResolvedValue(tenantPage)
    })

    it('renders the global tenant directory', async () => {
        renderTenants()

        expect(await screen.findByText('Research Lab')).toBeInTheDocument()
        expect(screen.getByText('research-lab')).toBeInTheDocument()
        expect(screen.getByText('Active')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /change status/i })).toBeInTheDocument()
    })

    it('submits search, status, and sorting parameters', async () => {
        const user = userEvent.setup()
        const getTenants = vi.mocked(systemAdminApi.getTenants)
        renderTenants()
        await screen.findByText('Research Lab')

        fireEvent.change(screen.getByLabelText(/search tenants/i), {
            target: { value: '  research  ' },
        })
        await user.click(screen.getByLabelText(/^status$/i))
        await user.click(screen.getByRole('option', { name: 'Suspended' }))
        await user.click(screen.getByRole('button', { name: /^search$/i }))
        await user.click(screen.getByRole('button', { name: /^tenant$/i }))

        expect(getTenants).toHaveBeenLastCalledWith(
            expect.objectContaining({
                page: 0,
                search: 'research',
                sortBy: 'name',
                sortDir: 'asc',
                status: 'SUSPENDED',
            }),
        )
    })

    it('changes tenant status through the protected mutation', async () => {
        const user = userEvent.setup()
        const updateStatus = vi.spyOn(systemAdminApi, 'updateTenantStatus')
            .mockResolvedValue({ ...tenant, status: 'SUSPENDED' })
        renderTenants()
        await screen.findByText('Research Lab')

        await user.click(screen.getByRole('button', { name: /change status/i }))
        const dialog = screen.getByRole('dialog', { name: /change tenant status/i })
        await user.click(within(dialog).getByLabelText(/^status$/i))
        await user.click(screen.getByRole('option', { name: 'Suspended' }))
        await user.click(within(dialog).getByRole('button', { name: /save status/i }))

        expect(updateStatus).toHaveBeenCalledWith(
            'tenant-1',
            { status: 'SUSPENDED' },
        )
        expect(await screen.findByText(/research lab is now suspended/i)).toBeInTheDocument()
    })

    it('normalizes and submits administrative tenant onboarding', async () => {
        const user = userEvent.setup()
        const onboard = vi.spyOn(systemAdminApi, 'onboardTenant')
            .mockResolvedValue({
                tenant,
                adminUser: {
                    id: 'user-1',
                    tenantId: tenant.id,
                    fullName: 'Grace Admin',
                    email: 'grace@example.com',
                    role: 'TENANT_ADMIN',
                    status: 'ACTIVE',
                    createdAt: tenant.createdAt,
                    updatedAt: tenant.updatedAt,
                },
                message: 'Tenant onboarded successfully by system admin',
            })
        renderTenants()
        await screen.findByText('Research Lab')
        await user.click(screen.getByRole('button', { name: /onboard tenant/i }))

        const dialog = screen.getByRole('dialog', { name: /onboard tenant/i })
        fireEvent.change(within(dialog).getByLabelText(/workspace name/i), { target: { value: '  Research Lab  ' } })
        fireEvent.change(within(dialog).getByLabelText(/workspace slug/i), { target: { value: '  Research-Lab  ' } })
        fireEvent.change(within(dialog).getByLabelText(/administrator full name/i), { target: { value: '  Grace Admin  ' } })
        fireEvent.change(within(dialog).getByLabelText(/administrator email address/i), { target: { value: '  GRACE@EXAMPLE.COM  ' } })
        fireEvent.change(within(dialog).getByLabelText(/^administrator password$/i), { target: { value: 'Strong@123' } })
        fireEvent.change(within(dialog).getByLabelText(/confirm administrator password/i), { target: { value: 'Strong@123' } })
        await user.click(within(dialog).getByRole('button', { name: /^onboard tenant$/i }))

        expect(onboard).toHaveBeenCalledWith({
            tenantName: 'Research Lab',
            tenantSlug: 'research-lab',
            adminFullName: 'Grace Admin',
            adminEmail: 'grace@example.com',
            adminPassword: 'Strong@123',
        })
        expect(await screen.findByText('tenant-1')).toBeInTheDocument()
        expect(screen.getByText('grace@example.com')).toBeInTheDocument()
    })
})
