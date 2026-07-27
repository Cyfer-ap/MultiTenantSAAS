import { ThemeProvider } from '@mui/material'
import {
    render,
    screen,
} from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { MemoryRouter } from 'react-router'

import App from './App'
import { AuthProvider } from './features/auth/context/AuthProvider'
import { appTheme } from './theme/appTheme'

describe('App authentication routes', () => {
    beforeEach(() => {
        localStorage.clear()
    })

    it(
        'redirects an unauthenticated user to login',
        async () => {
            render(
                <ThemeProvider theme={appTheme}>
                    <MemoryRouter
                        initialEntries={['/dashboard']}
                    >
                        <AuthProvider>
                            <App />
                        </AuthProvider>
                    </MemoryRouter>
                </ThemeProvider>,
            )

            expect(
                await screen.findByRole('heading', {
                    name: /sign in/i,
                }),
            ).toBeInTheDocument()

            expect(
                screen.getByLabelText(/tenant id/i),
            ).toBeInTheDocument()
        },
    )
})