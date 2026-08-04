import {
    describe,
    expect,
    it,
} from 'vitest'

import { getAvailableWorkspaceNavigationItems } from '../../../layouts/workspaceNavigation'
import { createTenantAuthorizationContext } from '../test/authorizationTestData'
import { authorizationPermissionCodes } from '../types/authorization'
import { getDefaultAuthorizedPath } from './authorizationAccess'

describe('authorization management access', () => {
    it('exposes authorization navigation from the V2 permission', () => {
        const context = createTenantAuthorizationContext({
            permissionCodes: [
                authorizationPermissionCodes
                    .AUTHORIZATION_MANAGE,
            ],
        })

        expect(
            getAvailableWorkspaceNavigationItems(context)
                .map((item) => item.label),
        ).toEqual(['Authorization'])

        expect(getDefaultAuthorizedPath(context))
            .toBe('/authorization')
    })
})
