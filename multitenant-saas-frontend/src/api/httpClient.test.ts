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
    vi,
} from 'vitest'

import { authStorage } from '../features/auth/storage/authStorage'
import type {
    AuthSession,
    TokenRefreshResponse,
} from '../features/auth/types/auth'
import type {
    ApiErrorResponse,
    ApiResponse,
} from '../types/api'
import {
    httpClient,
    publicHttpClient,
} from './httpClient'

const initialSession: AuthSession = {
    accessToken: 'expired-access-token',
    refreshToken: 'valid-refresh-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: 0,
    tenantId: 'tenant-id',
    userId: 'user-id',
    fullName: 'Tenant Admin',
    email: 'admin@example.com',
    role: 'TENANT_ADMIN',
}

const refreshedTokenData: TokenRefreshResponse = {
    accessToken: 'new-access-token',
    refreshToken: 'new-refresh-token',
    tokenType: 'Bearer',
    expiresInSeconds: 900,
}

function createUnauthorizedError(
    config: InternalAxiosRequestConfig,
): AxiosError<ApiErrorResponse> {
    const response: AxiosResponse<ApiErrorResponse> = {
        data: {
            success: false,
            message: 'Authentication is required.',
            errorCode: 'AUTHENTICATION_REQUIRED',
            status: 401,
            path: '/api/protected',
            details: null,
            timestamp: new Date().toISOString(),
        },
        status: 401,
        statusText: 'Unauthorized',
        headers: new AxiosHeaders(),
        config,
    }

    return new AxiosError(
        'Request failed with status code 401',
        AxiosError.ERR_BAD_REQUEST,
        config,
        undefined,
        response,
    )
}

function protectedResourceAdapter(
    config: InternalAxiosRequestConfig,
): Promise<AxiosResponse<{ value: string }>> {
    const authorization =
        config.headers.get('Authorization')

    if (
        authorization !==
        'Bearer new-access-token'
    ) {
        return Promise.reject(
            createUnauthorizedError(config),
        )
    }

    return Promise.resolve({
        data: {
            value: 'protected data',
        },
        status: 200,
        statusText: 'OK',
        headers: new AxiosHeaders(),
        config,
    })
}

describe('authenticated HTTP client', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        authStorage.clear()
        authStorage.write(initialSession)
    })

    it(
        'shares one refresh request across concurrent 401 responses',
        async () => {
            const refreshResponse: AxiosResponse<
                ApiResponse<TokenRefreshResponse>
            > = {
                data: {
                    success: true,
                    message:
                        'Token refreshed successfully',
                    data: refreshedTokenData,
                    timestamp:
                        new Date().toISOString(),
                },
                status: 200,
                statusText: 'OK',
                headers: new AxiosHeaders(),
                config: {
                    headers: new AxiosHeaders(),
                },
            }

            const refreshSpy = vi
                .spyOn(publicHttpClient, 'post')
                .mockResolvedValue(refreshResponse)

            const [firstResponse, secondResponse] =
                await Promise.all([
                    httpClient.get(
                        '/api/protected/one',
                        {
                            adapter:
                                protectedResourceAdapter,
                        },
                    ),
                    httpClient.get(
                        '/api/protected/two',
                        {
                            adapter:
                                protectedResourceAdapter,
                        },
                    ),
                ])

            expect(refreshSpy).toHaveBeenCalledTimes(1)
            expect(firstResponse.data.value).toBe(
                'protected data',
            )
            expect(secondResponse.data.value).toBe(
                'protected data',
            )
            expect(
                authStorage.read()?.accessToken,
            ).toBe('new-access-token')
            expect(
                authStorage.read()?.refreshToken,
            ).toBe('new-refresh-token')
        },
    )

    it(
        'clears the session when refresh-token validation fails',
        async () => {
            const refreshConfig: InternalAxiosRequestConfig =
                {
                    headers: new AxiosHeaders(),
                }

            vi.spyOn(
                publicHttpClient,
                'post',
            ).mockRejectedValue(
                createUnauthorizedError(refreshConfig),
            )

            await expect(
                httpClient.get('/api/protected', {
                    adapter:
                        protectedResourceAdapter,
                }),
            ).rejects.toMatchObject({
                status: 401,
            })

            expect(authStorage.read()).toBeNull()
        },
    )
})
