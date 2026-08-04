import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded'
import FolderRoundedIcon from '@mui/icons-material/FolderRounded'
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded'
import type { ReactNode } from 'react'

import {
    dashboardRequiredTenantPermissions,
    hasAllTenantPermissions,
    hasAnyTenantPermission,
} from '../features/authorization/access/authorizationAccess'
import {
    authorizationPermissionCodes,
    type CurrentAuthorizationContext,
} from '../features/authorization/types/authorization'

export interface WorkspaceNavigationItem {
    label: string
    path: string
    icon: ReactNode
    requiredTenantPermissions:
        readonly string[]
    match?: 'all' | 'any'
}

export const workspaceNavigationItems:
    readonly WorkspaceNavigationItem[] = [
        {
            label: 'Dashboard',
            path: '/dashboard',
            icon: <DashboardRoundedIcon />,
            requiredTenantPermissions:
                dashboardRequiredTenantPermissions,
        },
        {
            label: 'Users',
            path: '/users',
            icon: <GroupsRoundedIcon />,
            requiredTenantPermissions: [
                authorizationPermissionCodes.USER_READ,
            ],
        },
        {
            label: 'Projects',
            path: '/projects',
            icon: <FolderRoundedIcon />,
            requiredTenantPermissions: [
                authorizationPermissionCodes.PROJECT_READ,
            ],
        },
        {
            label: 'Invitations',
            path: '/invitations',
            icon: <PersonAddAltRoundedIcon />,
            requiredTenantPermissions: [
                authorizationPermissionCodes.USER_READ,
                authorizationPermissionCodes.USER_CREATE,
            ],
        },
        {
            label: 'Audit Logs',
            path: '/audit-logs',
            icon: <HistoryRoundedIcon />,
            requiredTenantPermissions: [
                authorizationPermissionCodes.AUDIT_READ,
            ],
        },
    ]

export function getAvailableWorkspaceNavigationItems(
    context: CurrentAuthorizationContext,
): readonly WorkspaceNavigationItem[] {
    return workspaceNavigationItems.filter(
        (item) =>
            item.match === 'any'
                ? hasAnyTenantPermission(
                    context,
                    item.requiredTenantPermissions,
                )
                : hasAllTenantPermissions(
                    context,
                    item.requiredTenantPermissions,
                ),
    )
}
