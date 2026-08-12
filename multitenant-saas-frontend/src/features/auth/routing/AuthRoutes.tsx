import { Box, CircularProgress } from '@mui/material'
import { Navigate, Outlet, useLocation } from 'react-router'

import { useAuth } from '../hooks/useAuth'

function AuthenticationLoadingScreen() {
    return (
        <Box
            sx={{
                display: 'grid',
                minHeight: '100vh',
                placeItems: 'center',
            }}
        >
            <CircularProgress aria-label="Loading session" />
        </Box>
    )
}

export function ProtectedRoute() {
    const { session, status } = useAuth()
    const location = useLocation()

    if (status === 'loading') {
        return <AuthenticationLoadingScreen />
    }

    if (status === 'unauthenticated' || !session) {
        const redirectPath = [location.pathname, location.search, location.hash].join('')

        return (
            <Navigate
                to="/login"
                replace
                state={{
                    from: redirectPath,
                }}
            />
        )
    }

    return <Outlet />
}

export function PublicOnlyRoute() {
    const { session, status } = useAuth()

    if (status === 'loading') {
        return <AuthenticationLoadingScreen />
    }

    if (status === 'authenticated' && session) {
        return <Navigate to="/" replace />
    }

    return <Outlet />
}
