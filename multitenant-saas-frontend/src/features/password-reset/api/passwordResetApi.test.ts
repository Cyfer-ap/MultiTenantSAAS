import { beforeEach, describe, expect, it, vi } from 'vitest'

import { publicHttpClient } from '../../../api/httpClient'
import { passwordResetApi } from './passwordResetApi'

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-18T00:00:00Z',
        },
    }
}

describe('passwordResetApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('requests a reset link through the verified public endpoint', async () => {
        const response = {
            message: 'Password reset instructions will be sent.',
            devResetToken: null,
        }
        const input = {
            tenantId: 'tenant-1',
            email: 'grace@example.com',
            workspaceGrantId: 'grant-1',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(passwordResetApi.forgotPassword(input)).resolves.toEqual(response)

        expect(post).toHaveBeenCalledWith('/api/auth/password-reset/request', input)
    })

    it('completes a reset through the token-only public endpoint', async () => {
        const response = {
            message: 'Password reset successfully.',
        }
        const input = {
            resetToken: 'reset-token',
            newPassword: 'Stronger@456',
            confirmPassword: 'Stronger@456',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(passwordResetApi.resetPassword(input)).resolves.toEqual(response)

        expect(post).toHaveBeenCalledWith('/api/auth/password-reset/complete', input)
    })
})
