import { Box, Paper, Tab, Tabs } from '@mui/material'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router'

import { hasTenantPermission } from '../features/authorization/access/authorizationAccess'
import { useCurrentAuthorization } from '../features/authorization/hooks/useCurrentAuthorization'
import { authorizationPermissionCodes } from '../features/authorization/types/authorization'

const authorizationPaths = {
    manage: '/authorization/manage',
    delegations: '/authorization/delegations',
    explain: '/authorization/explain',
} as const

export function AuthorizationWorkspaceLayout() {
    const authorization = useCurrentAuthorization()
    const location = useLocation()
    const navigate = useNavigate()
    const canManage = hasTenantPermission(
        authorization.data,
        authorizationPermissionCodes.AUTHORIZATION_MANAGE,
    )

    const currentPath = Object.values(authorizationPaths).includes(
        location.pathname as (typeof authorizationPaths)[keyof typeof authorizationPaths],
    )
        ? location.pathname
        : false

    return (
        <Box>
            <Paper sx={{ marginBottom: 2 }} variant="outlined">
                <Tabs
                    onChange={(_event, value: string) => {
                        void navigate(value)
                    }}
                    value={currentPath}
                    variant="scrollable"
                >
                    {canManage && <Tab label="Management" value={authorizationPaths.manage} />}
                    <Tab label="Delegations" value={authorizationPaths.delegations} />
                    {canManage && <Tab label="Explain access" value={authorizationPaths.explain} />}
                </Tabs>
            </Paper>
            <Outlet />
        </Box>
    )
}

export function AuthorizationWorkspaceRedirect() {
    const authorization = useCurrentAuthorization()
    const canManage = hasTenantPermission(
        authorization.data,
        authorizationPermissionCodes.AUTHORIZATION_MANAGE,
    )

    return (
        <Navigate
            replace
            to={canManage ? authorizationPaths.manage : authorizationPaths.delegations}
        />
    )
}
