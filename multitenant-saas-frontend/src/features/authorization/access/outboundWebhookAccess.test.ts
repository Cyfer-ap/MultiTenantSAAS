import { describe, expect, it } from 'vitest'

import { getAvailableWorkspaceNavigationItems } from '../../../layouts/workspaceNavigation'
import { createTenantAuthorizationContext } from '../test/authorizationTestData'
import { authorizationPermissionCodes } from '../types/authorization'
import { getDefaultAuthorizedPath } from './authorizationAccess'

describe('outbound webhook access', () => {
    it('exposes integrations and uses it as the default route for tenant.update', () => {
        const context = createTenantAuthorizationContext({
            permissionCodes: [authorizationPermissionCodes.TENANT_UPDATE],
        })

        expect(getAvailableWorkspaceNavigationItems(context).map((item) => item.label)).toEqual([
            'Integrations',
        ])
        expect(getDefaultAuthorizedPath(context)).toBe('/integrations')
    })

    it('does not expose integrations without tenant.update', () => {
        const context = createTenantAuthorizationContext({
            permissionCodes: [authorizationPermissionCodes.SUBSCRIPTION_READ],
        })

        expect(
            getAvailableWorkspaceNavigationItems(context).map((item) => item.label),
        ).not.toContain('Integrations')
    })
})
