import { Alert, Box, Button, CircularProgress, Stack } from '@mui/material'
import { Navigate, Outlet, useParams } from 'react-router'

import {
    getDefaultAuthorizedPath,
    hasAllTenantPermissions,
    hasAnyTenantPermission,
    hasProjectPermission,
} from '../access/authorizationAccess'
import { useCurrentAuthorization } from '../hooks/useCurrentAuthorization'

function AuthorizationLoadingScreen() {
    return (
        <Box
            sx={{
                display: 'grid',
                minHeight: 240,
                placeItems: 'center',
            }}
        >
            <CircularProgress aria-label="Loading authorization" />
        </Box>
    )
}

interface AuthorizationLoadErrorProps {
    message: string
    retry: () => void
}

function AuthorizationLoadError({ message, retry }: AuthorizationLoadErrorProps) {
    return (
        <Stack
            spacing={2}
            sx={{
                maxWidth: 640,
            }}
        >
            <Alert severity="error">{message}</Alert>

            <Button
                variant="outlined"
                onClick={retry}
                sx={{
                    alignSelf: 'flex-start',
                }}
            >
                Retry
            </Button>
        </Stack>
    )
}

type PermissionMatchMode = 'all' | 'any'

interface TenantPermissionProtectedRouteProps {
    requiredPermissions: readonly string[]
    match?: PermissionMatchMode
}

export function TenantPermissionProtectedRoute({
    requiredPermissions,
    match = 'all',
}: TenantPermissionProtectedRouteProps) {
    const authorization = useCurrentAuthorization()

    if (authorization.isPending) {
        return <AuthorizationLoadingScreen />
    }

    if (authorization.isError || !authorization.data) {
        return (
            <AuthorizationLoadError
                message={
                    authorization.error?.message ?? 'Unable to load authorization permissions.'
                }
                retry={() => {
                    void authorization.refetch()
                }}
            />
        )
    }

    const permitted =
        match === 'all'
            ? hasAllTenantPermissions(authorization.data, requiredPermissions)
            : hasAnyTenantPermission(authorization.data, requiredPermissions)

    if (!permitted) {
        return <Navigate to={getDefaultAuthorizedPath(authorization.data)} replace />
    }

    return <Outlet />
}

interface ProjectPermissionProtectedRouteProps {
    permissionCode: string
}

export function ProjectPermissionProtectedRoute({
    permissionCode,
}: ProjectPermissionProtectedRouteProps) {
    const { projectId } = useParams()
    const authorization = useCurrentAuthorization()

    if (authorization.isPending) {
        return <AuthorizationLoadingScreen />
    }

    if (authorization.isError || !authorization.data) {
        return (
            <AuthorizationLoadError
                message={
                    authorization.error?.message ?? 'Unable to load authorization permissions.'
                }
                retry={() => {
                    void authorization.refetch()
                }}
            />
        )
    }

    if (!projectId || !hasProjectPermission(authorization.data, permissionCode, projectId)) {
        return <Navigate to={getDefaultAuthorizedPath(authorization.data)} replace />
    }

    return <Outlet />
}

export function AuthorizationHomeRedirect() {
    const authorization = useCurrentAuthorization()

    if (authorization.isPending) {
        return <AuthorizationLoadingScreen />
    }

    if (authorization.isError || !authorization.data) {
        return (
            <AuthorizationLoadError
                message={
                    authorization.error?.message ?? 'Unable to load authorization permissions.'
                }
                retry={() => {
                    void authorization.refetch()
                }}
            />
        )
    }

    return <Navigate to={getDefaultAuthorizedPath(authorization.data)} replace />
}
