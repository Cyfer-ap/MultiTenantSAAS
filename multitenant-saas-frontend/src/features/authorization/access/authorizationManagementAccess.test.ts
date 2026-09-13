import { describe, expect, it } from 'vitest'

import { getAvailableWorkspaceNavigationItems } from '../../../layouts/workspaceNavigation'
import { createTenantAuthorizationContext } from '../test/authorizationTestData'
import { authorizationPermissionCodes } from '../types/authorization'
import { getDefaultAuthorizedPath } from './authorizationAccess'

describe('authorization workspace access', () => {
    it('exposes authorization navigation to authorization managers', () => {
        const context = createTenantAuthorizationContext({
            permissionCodes: [authorizationPermissionCodes.AUTHORIZATION_MANAGE],
        })

        expect(getAvailableWorkspaceNavigationItems(context).map((item) => item.label)).toEqual([
            'Authorization',
        ])
        expect(getDefaultAuthorizedPath(context)).toBe('/authorization')
    })

    it('exposes authorization navigation to delegate-only users', () => {
        const context = createTenantAuthorizationContext({
            permissionCodes: [authorizationPermissionCodes.AUTHORIZATION_DELEGATE],
        })

        expect(getAvailableWorkspaceNavigationItems(context).map((item) => item.label)).toEqual([
            'Authorization',
        ])
        expect(getDefaultAuthorizedPath(context)).toBe('/authorization')
    })
})
