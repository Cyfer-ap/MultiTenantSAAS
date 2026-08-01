import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { authApi } from './authApi'

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-02T12:00:00Z',
        },
    }
}

describe('authApi account operations', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads the authenticated tenant account', async () => {
        const currentUser = {
            tenantId: 'tenant-1',
            tenantName: 'Research Lab',
            tenantSlug: 'research-lab',
            userId: 'user-1',
            fullName: 'Grace Hopper',
            email: 'grace@example.com',
            role: 'TENANT_ADMIN' as const,
            status: 'ACTIVE' as const,
        }
        const get = vi.spyOn(httpClient, 'get')
            .mockResolvedValue(successfulResponse(currentUser))

        await expect(authApi.getCurrentUser())
            .resolves.toEqual(currentUser)
        expect(get).toHaveBeenCalledWith('/api/auth/me')
    })

    it('changes the password through the protected tenant endpoint', async () => {
        const response = {
            message: 'Password changed successfully. Please login again.',
        }
        const input = {
            currentPassword: 'Current@123',
            newPassword: 'Stronger@456',
            confirmPassword: 'Stronger@456',
        }
        const post = vi.spyOn(httpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(authApi.changePassword(input))
            .resolves.toEqual(response)
        expect(post).toHaveBeenCalledWith(
            '/api/auth/change-password',
            input,
        )
    })

    it('revokes every device refresh session through the protected tenant endpoint', async () => {
        const response = {
            message: 'Logged out from all devices successfully',
        }
        const post = vi.spyOn(httpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(authApi.logoutAllDevices())
            .resolves.toEqual(response)
        expect(post).toHaveBeenCalledWith(
            '/api/auth/logout-all',
        )
    })
})
