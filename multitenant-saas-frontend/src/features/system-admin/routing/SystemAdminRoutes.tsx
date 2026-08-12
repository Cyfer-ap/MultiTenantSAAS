import { Box, CircularProgress } from '@mui/material'
import { Navigate, Outlet, useLocation } from 'react-router'

import { useSystemAdmin } from '../hooks/useSystemAdmin'

function SystemAuthenticationLoadingScreen() {
    return (
        <Box
            sx={{
                display: 'grid',
                minHeight: '100vh',
                placeItems: 'center',
            }}
        >
            <CircularProgress aria-label="Loading system session" />
        </Box>
    )
}

export function SystemProtectedRoute() {
    const { session, status } = useSystemAdmin()
    const location = useLocation()

    if (status === 'loading') {
        return <SystemAuthenticationLoadingScreen />
    }

    if (status === 'unauthenticated' || !session) {
        return (
            <Navigate
                replace
                state={{
                    from: [location.pathname, location.search, location.hash].join(''),
                }}
                to="/system/login"
            />
        )
    }

    return <Outlet />
}

export function SystemPublicOnlyRoute() {
    const { session, status } = useSystemAdmin()

    if (status === 'loading') {
        return <SystemAuthenticationLoadingScreen />
    }

    if (status === 'authenticated' && session) {
        return <Navigate replace to="/system/dashboard" />
    }

    return <Outlet />
}

export function SystemHomeRedirect() {
    return <Navigate replace to="/system/dashboard" />
}
