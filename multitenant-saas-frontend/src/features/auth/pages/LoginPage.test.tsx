import { ThemeProvider } from '@mui/material'
import {
    render,
    screen,
} from '@testing-library/react'
import {
    describe,
    expect,
    it,
    vi,
} from 'vitest'
import { MemoryRouter } from 'react-router'

import { appTheme } from '../../../theme/appTheme'
import { AuthContext } from '../context/AuthContext'
import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
    it('prefills tenant details supplied by onboarding', () => {
        render(
            <ThemeProvider theme={appTheme}>
                <AuthContext.Provider
                    value={{
                        status: 'unauthenticated',
                        session: null,
                        login: vi.fn(),
                        logout: vi.fn(),
                    }}
                >
                    <MemoryRouter
                        initialEntries={[
                            {
                                pathname: '/login',
                                state: {
                                    tenantId: 'tenant-1',
                                    email: 'grace@example.com',
                                },
                            },
                        ]}
                    >
                        <LoginPage />
                    </MemoryRouter>
                </AuthContext.Provider>
            </ThemeProvider>,
        )

        expect(screen.getByLabelText(/tenant id/i)).toHaveValue(
            'tenant-1',
        )
        expect(screen.getByLabelText(/email address/i)).toHaveValue(
            'grace@example.com',
        )
    })
})
