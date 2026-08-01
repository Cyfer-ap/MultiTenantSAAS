import { ThemeProvider } from '@mui/material'
import {
    fireEvent,
    render,
    screen,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
    describe,
    expect,
    it,
    vi,
} from 'vitest'
import {
    MemoryRouter,
    Route,
    Routes,
} from 'react-router'

import { SystemAdminContext } from '../features/system-admin/context/SystemAdminContext'
import { appTheme } from '../theme/appTheme'
import { SystemLoginPage } from './SystemLoginPage'

function renderLogin(login: (input: { email: string; password: string }) => Promise<void>) {
    return render(
        <ThemeProvider theme={appTheme}>
            <SystemAdminContext.Provider value={{
                status: 'unauthenticated',
                session: null,
                login,
                logout: vi.fn(),
            }}>
                <MemoryRouter
                    initialEntries={[{
                        pathname: '/system/login',
                        state: { from: '/system/tenants?status=ACTIVE' },
                    }]}
                >
                    <Routes>
                        <Route path="system/login" element={<SystemLoginPage />} />
                        <Route path="system/tenants" element={<output>Tenant destination</output>} />
                    </Routes>
                </MemoryRouter>
            </SystemAdminContext.Provider>
        </ThemeProvider>,
    )
}

describe('SystemLoginPage', () => {
    it('normalizes credentials and returns to the requested system route', async () => {
        const user = userEvent.setup()
        const login = vi.fn().mockResolvedValue(undefined)
        renderLogin(login)

        fireEvent.change(screen.getByLabelText(/email address/i), {
            target: { value: '  OWNER@EXAMPLE.COM  ' },
        })
        fireEvent.change(screen.getByLabelText(/^password$/i), {
            target: { value: 'Strong@123' },
        })
        await user.click(screen.getByRole('button', {
            name: /sign in to system console/i,
        }))

        expect(login).toHaveBeenCalledWith({
            email: 'owner@example.com',
            password: 'Strong@123',
        })
        expect(await screen.findByText('Tenant destination')).toBeInTheDocument()
    })

    it('renders authentication errors without changing route', async () => {
        const user = userEvent.setup()
        renderLogin(vi.fn().mockRejectedValue(new Error('Invalid email or password')))

        fireEvent.change(screen.getByLabelText(/email address/i), {
            target: { value: 'owner@example.com' },
        })
        fireEvent.change(screen.getByLabelText(/^password$/i), {
            target: { value: 'Wrong@123' },
        })
        await user.click(screen.getByRole('button', {
            name: /sign in to system console/i,
        }))

        expect(await screen.findByText('Invalid email or password')).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: /system console/i })).toBeInTheDocument()
    })
})
