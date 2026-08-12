import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'

import { ApiClientError } from '../api/apiError'
import { onboardingApi } from '../features/onboarding/api/onboardingApi'
import { appTheme } from '../theme/appTheme'
import { TenantOnboardingPage } from './TenantOnboardingPage'

const onboardingResponse = {
    tenant: {
        id: 'tenant-1',
        name: 'Research Workspace',
        slug: 'research-workspace',
        status: 'ACTIVE' as const,
        createdAt: '2026-08-01T12:00:00Z',
        updatedAt: '2026-08-01T12:00:00Z',
    },
    adminUser: {
        id: 'user-1',
        tenantId: 'tenant-1',
        fullName: 'Grace Admin',
        email: 'grace@example.com',
        role: 'TENANT_ADMIN' as const,
        status: 'ACTIVE' as const,
        createdAt: '2026-08-01T12:00:00Z',
        updatedAt: '2026-08-01T12:00:00Z',
    },
    message: 'Tenant onboarded successfully.',
}

function LoginStateProbe() {
    const location = useLocation()

    return <output aria-label="login route state">{JSON.stringify(location.state)}</output>
}

function renderOnboardingPage() {
    const queryClient = new QueryClient({
        defaultOptions: {
            mutations: {
                retry: false,
            },
        },
    })

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/register']}>
                        <Routes>
                            <Route path="register" element={children} />
                            <Route path="login" element={<LoginStateProbe />} />
                        </Routes>
                    </MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<TenantOnboardingPage />, {
        wrapper: Wrapper,
    })
}

function completeForm(): void {
    fireEvent.change(screen.getByLabelText(/workspace name/i), {
        target: { value: '  Research Workspace  ' },
    })
    fireEvent.change(screen.getByLabelText(/workspace slug/i), {
        target: { value: '  Research-Workspace  ' },
    })
    fireEvent.change(screen.getByLabelText(/full name/i), {
        target: { value: '  Grace Admin  ' },
    })
    fireEvent.change(screen.getByLabelText(/email address/i), {
        target: { value: '  GRACE@EXAMPLE.COM  ' },
    })
    fireEvent.change(screen.getByLabelText(/^password/i), {
        target: { value: 'Strong@123' },
    })
    fireEvent.change(screen.getByLabelText(/confirm password/i), {
        target: { value: 'Strong@123' },
    })
}

describe('TenantOnboardingPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('validates the onboarding form before calling the API', async () => {
        const user = userEvent.setup()
        const onboardTenant = vi.spyOn(onboardingApi, 'onboardTenant')

        renderOnboardingPage()
        fireEvent.change(screen.getByLabelText(/workspace name/i), {
            target: { value: 'A' },
        })
        await user.click(
            screen.getByRole('button', {
                name: /^create workspace$/i,
            }),
        )

        expect(
            await screen.findByText(/workspace name must be between 2 and 100 characters/i),
        ).toBeInTheDocument()
        expect(onboardTenant).not.toHaveBeenCalled()
    })

    it('normalizes the request and hands login details to the sign-in route', async () => {
        const user = userEvent.setup()
        const onboardTenant = vi
            .spyOn(onboardingApi, 'onboardTenant')
            .mockResolvedValue(onboardingResponse)

        renderOnboardingPage()
        completeForm()
        await user.click(
            screen.getByRole('button', {
                name: /^create workspace$/i,
            }),
        )

        expect(
            await screen.findByRole('heading', {
                name: /workspace created/i,
            }),
        ).toBeInTheDocument()
        expect(screen.getByText('tenant-1')).toBeInTheDocument()
        expect(screen.getByText('grace@example.com')).toBeInTheDocument()
        expect(onboardTenant).toHaveBeenCalledWith({
            tenantName: 'Research Workspace',
            tenantSlug: 'research-workspace',
            adminFullName: 'Grace Admin',
            adminEmail: 'grace@example.com',
            adminPassword: 'Strong@123',
        })

        await user.click(
            screen.getByRole('button', {
                name: /continue to sign in/i,
            }),
        )

        expect(screen.getByLabelText(/login route state/i)).toHaveTextContent(
            JSON.stringify({
                tenantId: 'tenant-1',
                email: 'grace@example.com',
            }),
        )
    })

    it('surfaces a duplicate workspace slug returned by the backend', async () => {
        const user = userEvent.setup()
        vi.spyOn(onboardingApi, 'onboardTenant').mockRejectedValue(
            new ApiClientError({
                message: 'Tenant already exists with slug: research-workspace',
                errorCode: 'RESOURCE_ALREADY_EXISTS',
                status: 409,
            }),
        )

        renderOnboardingPage()
        completeForm()
        await user.click(
            screen.getByRole('button', {
                name: /^create workspace$/i,
            }),
        )

        expect(await screen.findByText(/tenant already exists with slug/i)).toBeInTheDocument()
    })
})
