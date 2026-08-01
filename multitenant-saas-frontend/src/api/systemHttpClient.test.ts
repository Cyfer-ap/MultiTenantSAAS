import {
    AxiosError,
    AxiosHeaders,
} from 'axios'
import type {
    AxiosResponse,
    InternalAxiosRequestConfig,
} from 'axios'
import {
    beforeEach,
    describe,
    expect,
    it,
} from 'vitest'

import { authStorage } from '../features/auth/storage/authStorage'
import { systemAdminStorage } from '../features/system-admin/storage/systemAdminStorage'
import type { ApiErrorResponse } from '../types/api'
import { systemHttpClient } from './systemHttpClient'

const systemSession = {
    systemAdminId: 'system-admin-1',
    fullName: 'Platform Owner',
    email: 'owner@example.com',
    role: 'SYSTEM_ADMIN' as const,
    accessToken: 'system-access-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
}

const tenantSession = {
    accessToken: 'tenant-access-token',
    refreshToken: 'tenant-refresh-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
    tenantId: 'tenant-1',
    userId: 'user-1',
    fullName: 'Tenant Admin',
    email: 'admin@example.com',
    role: 'TENANT_ADMIN' as const,
}

function forbiddenError(
    config: InternalAxiosRequestConfig,
): AxiosError<ApiErrorResponse> {
    const response: AxiosResponse<ApiErrorResponse> = {
        data: {
            success: false,
            message: 'System administrator access is required.',
            errorCode: 'ACCESS_DENIED',
            status: 403,
            path: '/api/tenants',
            details: null,
            timestamp: new Date().toISOString(),
        },
        status: 403,
        statusText: 'Forbidden',
        headers: new AxiosHeaders(),
        config,
    }

    return new AxiosError(
        'Request failed with status code 403',
        AxiosError.ERR_BAD_REQUEST,
        config,
        undefined,
        response,
    )
}

describe('system HTTP client', () => {
    beforeEach(() => {
        localStorage.clear()
        authStorage.write(tenantSession)
        systemAdminStorage.write(systemSession)
    })

    it('sends only the dedicated system administrator token', async () => {
        let authorization: unknown

        await systemHttpClient.get('/api/dashboard/summary', {
            adapter: (config) => {
                authorization = config.headers.get('Authorization')
                return Promise.resolve({
                    data: { value: 'ok' },
                    status: 200,
                    statusText: 'OK',
                    headers: new AxiosHeaders(),
                    config,
                })
            },
        })

        expect(authorization).toBe('Bearer system-access-token')
        expect(authStorage.read()?.accessToken).toBe('tenant-access-token')
    })

    it('clears only the system session after live authorization fails', async () => {
        await expect(
            systemHttpClient.get('/api/tenants', {
                adapter: (config) => Promise.reject(forbiddenError(config)),
            }),
        ).rejects.toMatchObject({ status: 403 })

        expect(systemAdminStorage.read()).toBeNull()
        expect(authStorage.read()?.accessToken).toBe('tenant-access-token')
    })
})
