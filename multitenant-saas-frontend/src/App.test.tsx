import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'

import App from './App'
import { appTheme } from './theme/appTheme'

describe('App', () => {
    it('renders the dashboard route inside the app shell', () => {
        render(
            <ThemeProvider theme={appTheme}>
                <MemoryRouter initialEntries={['/dashboard']}>
                    <App />
                </MemoryRouter>
            </ThemeProvider>,
        )

        expect(
            screen.getByRole('heading', {
                name: /dashboard/i,
            }),
        ).toBeInTheDocument()

        expect(
            screen.getAllByText(/multi-tenant saas/i).length,
        ).toBeGreaterThan(0)
    })
})