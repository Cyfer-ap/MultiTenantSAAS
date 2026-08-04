import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { CurrentAuthorizationContext } from '../types/authorization'
import { authorizationApi } from './authorizationApi'

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-04T12:00:00Z',
        },
    }
}

describe('authorizationApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads the current tenant authorization context', async () => {
        const context:
            CurrentAuthorizationContext = {
                tenantId: 'tenant-1',
                userId: 'user-1',
                fullName: 'Current User',
                email: 'user@example.com',
                evaluatedAt:
                    '2026-08-04T12:00:00Z',
                tenantPermissionCodes: [
                    'project.read',
                ],
                allPermissionCodes: [
                    'project.read',
                ],
                grants: [],
            }

        const get =
            vi.spyOn(httpClient, 'get')
                .mockResolvedValue(
                    successfulResponse(context),
                )

        await expect(
            authorizationApi
                .getCurrentAuthorizationContext(
                    'tenant-1',
                ),
        ).resolves.toEqual(context)

        expect(get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/authorization/me',
        )
    })
})
