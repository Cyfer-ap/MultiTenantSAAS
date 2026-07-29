import {
    Box,
    CircularProgress,
} from '@mui/material'
import {
    Navigate,
    Outlet,
    useLocation,
} from 'react-router'

import {
    getDefaultAuthenticatedPath,
    hasAllowedTenantRole,
} from '../access/roleAccess'
import { useAuth } from '../hooks/useAuth'
import type { TenantRole } from '../types/auth'

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

    if (
        status === 'unauthenticated' ||
        !session
    ) {
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

interface RoleProtectedRouteProps {
    allowedRoles: readonly TenantRole[]
}

export function RoleProtectedRoute({
    allowedRoles,
}: RoleProtectedRouteProps) {
    const { session, status } = useAuth()

    if (status === 'loading') {
        return <AuthenticationLoadingScreen />
    }

    if (
        status === 'unauthenticated' ||
        !session
    ) {
        return (
            <Navigate
                to="/login"
                replace
            />
        )
    }

    if (
        !hasAllowedTenantRole(
            session.role,
            allowedRoles,
        )
    ) {
        return (
            <Navigate
                to={getDefaultAuthenticatedPath(
                    session.role,
                )}
                replace
            />
        )
    }

    return <Outlet />
}

export function AuthenticatedHomeRedirect() {
    const { session, status } = useAuth()

    if (status === 'loading') {
        return <AuthenticationLoadingScreen />
    }

    if (
        status === 'unauthenticated' ||
        !session
    ) {
        return (
            <Navigate
                to="/login"
                replace
            />
        )
    }

    return (
        <Navigate
            to={getDefaultAuthenticatedPath(
                session.role,
            )}
            replace
        />
    )
}

export function PublicOnlyRoute() {
    const { session, status } = useAuth()

    if (status === 'loading') {
        return <AuthenticationLoadingScreen />
    }

    if (
        status === 'authenticated' &&
        session
    ) {
        return (
            <Navigate
                to={getDefaultAuthenticatedPath(
                    session.role,
                )}
                replace
            />
        )
    }

    return <Outlet />
}
