import { beforeEach, describe, expect, it, vi } from 'vitest'

import { publicHttpClient } from '../../../api/httpClient'
import { passwordResetApi } from './passwordResetApi'

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-01T12:00:00Z',
        },
    }
}

describe('passwordResetApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('requests a reset token through the tenant endpoint', async () => {
        const response = {
            message: 'Reset token generated.',
            devResetToken: 'reset-token',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(
            passwordResetApi.forgotPassword('tenant-1', { email: 'grace@example.com' }),
        ).resolves.toEqual(response)

        expect(post).toHaveBeenCalledWith('/api/tenants/tenant-1/auth/forgot-password', {
            email: 'grace@example.com',
        })
    })

    it('resets a password through the public endpoint', async () => {
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

        expect(post).toHaveBeenCalledWith('/api/auth/reset-password', input)
    })
})
