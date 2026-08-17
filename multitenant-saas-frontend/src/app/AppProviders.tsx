import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClientProvider } from '@tanstack/react-query'
import type { PropsWithChildren } from 'react'
import { useMemo } from 'react'
import { BrowserRouter } from 'react-router'

import { AuthProvider } from '../features/auth/context/AuthProvider'
import { SystemAdminProvider } from '../features/system-admin/context/SystemAdminProvider'
import { createAppTheme } from '../theme/appTheme'
import { useThemeMode } from '../theme/themeMode'
import { ThemeModeProvider } from '../theme/ThemeModeProvider'
import { queryClient } from './queryClient'

function ApplicationTheme({ children }: PropsWithChildren) {
    const { mode } = useThemeMode()
    const theme = useMemo(() => createAppTheme(mode), [mode])

    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            {children}
        </ThemeProvider>
    )
}

export function AppProviders({ children }: PropsWithChildren) {
    return (
        <ThemeModeProvider>
            <ApplicationTheme>
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <SystemAdminProvider>
                            <AuthProvider>{children}</AuthProvider>
                        </SystemAdminProvider>
                    </BrowserRouter>
                </QueryClientProvider>
            </ApplicationTheme>
        </ThemeModeProvider>
    )
}
