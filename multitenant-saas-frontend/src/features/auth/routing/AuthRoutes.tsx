import {
    Box,
    CircularProgress,
} from '@mui/material'
import {
    Navigate,
    Outlet,
    useLocation,
} from 'react-router'

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
    const { status } = useAuth()
    const location = useLocation()

    if (status === 'loading') {
        return <AuthenticationLoadingScreen />
    }

    if (status === 'unauthenticated') {
        const redirectPath = [
            location.pathname,
            location.search,
            location.hash,
        ].join('')

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
    const { status } = useAuth()

    if (status === 'loading') {
        return <AuthenticationLoadingScreen />
    }

    if (status === 'authenticated') {
        return (
            <Navigate
                to="/dashboard"
                replace
            />
        )
    }

    return <Outlet />
}