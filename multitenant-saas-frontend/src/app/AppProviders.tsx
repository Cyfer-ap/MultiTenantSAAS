import type { PropsWithChildren } from 'react'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router'

import { appTheme } from '../theme/appTheme'
import { queryClient } from './queryClient'

export function AppProviders({ children }: PropsWithChildren) {
    return (
        <ThemeProvider theme={appTheme}>
            <CssBaseline />

            <QueryClientProvider client={queryClient}>
                <BrowserRouter>{children}</BrowserRouter>
            </QueryClientProvider>
        </ThemeProvider>
    )
}