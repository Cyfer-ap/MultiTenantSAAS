import { describe, expect, it } from 'vitest'

import { getAvailableWorkspaceNavigationItems } from '../../../layouts/workspaceNavigation'
import { createTenantAuthorizationContext } from '../test/authorizationTestData'
import { authorizationPermissionCodes } from '../types/authorization'
import { getDefaultAuthorizedPath } from './authorizationAccess'

describe('tenant.update workspace access', () => {
    it('exposes authentication and integrations while preserving integrations as the default route', () => {
        const context = createTenantAuthorizationContext({
            permissionCodes: [authorizationPermissionCodes.TENANT_UPDATE],
        })

        expect(getAvailableWorkspaceNavigationItems(context).map((item) => item.label)).toEqual([
            'Authentication',
            'Integrations',
        ])
        expect(getDefaultAuthorizedPath(context)).toBe('/integrations')
    })

    it('does not expose authentication or integrations without tenant.update', () => {
        const context = createTenantAuthorizationContext({
            permissionCodes: [authorizationPermissionCodes.SUBSCRIPTION_READ],
        })
        const labels = getAvailableWorkspaceNavigationItems(context).map((item) => item.label)

        expect(labels).not.toContain('Authentication')
        expect(labels).not.toContain('Integrations')
    })
})
