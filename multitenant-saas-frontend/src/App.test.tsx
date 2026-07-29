import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    render,
    screen,
} from '@testing-library/react'
import {
    beforeEach,
    describe,
    expect,
    it,
} from 'vitest'
import { MemoryRouter } from 'react-router'

import App from './App'
import { AuthProvider } from './features/auth/context/AuthProvider'
import { appTheme } from './theme/appTheme'

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

describe('App authentication routes', () => {
    beforeEach(() => {
        localStorage.clear()
    })

    it(
        'redirects an unauthenticated user to login',
        async () => {
            const queryClient =
                createTestQueryClient()

            render(
                <ThemeProvider theme={appTheme}>
                    <QueryClientProvider
                        client={queryClient}
                    >
                        <MemoryRouter
                            initialEntries={['/dashboard']}
                        >
                            <AuthProvider>
                                <App />
                            </AuthProvider>
                        </MemoryRouter>
                    </QueryClientProvider>
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