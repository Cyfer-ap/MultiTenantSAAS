import axios from 'axios'
import type {
    AxiosError,
    AxiosInstance,
    AxiosResponse,
    InternalAxiosRequestConfig,
} from 'axios'

import { env } from '../config/env'
import { applyTokenRefresh } from '../features/auth/session/authSession'
import { authStorage } from '../features/auth/storage/authStorage'
import type {
    AuthSession,
    TokenRefreshResponse,
} from '../features/auth/types/auth'
import type { ApiResponse } from '../types/api'
import { normalizeApiError } from './apiError'

const clientConfiguration = {
    baseURL: env.apiBaseUrl,
    timeout: 15_000,
    headers: {
        Accept: 'application/json',
    },
}

export const publicHttpClient =
    axios.create(clientConfiguration)

export const httpClient =
    axios.create(clientConfiguration)

interface RetryableRequestConfig
    extends InternalAxiosRequestConfig {
    retriedAfterUnauthorized?: boolean
}

let refreshPromise: Promise<AuthSession> | null = null

httpClient.interceptors.request.use(
    (config) => {
        const session = authStorage.read()

        if (session) {
            config.headers.set(
                'Authorization',
                `${session.tokenType} ${session.accessToken}`,
            )
        }

        return config
    },
)

async function refreshSession(
    session: AuthSession,
): Promise<AuthSession> {
    const response =
        await publicHttpClient.post<
            ApiResponse<TokenRefreshResponse>
        >(
            '/api/auth/refresh',
            {
                refreshToken: session.refreshToken,
            },
        )

    const currentSession = authStorage.read()

    if (
        !currentSession ||
        currentSession.refreshToken !==
        session.refreshToken
    ) {
        throw new Error(
            'The authentication session changed during token refresh.',
        )
    }

    const refreshedSession = applyTokenRefresh(
        currentSession,
        response.data.data,
    )

    authStorage.write(refreshedSession)

    return refreshedSession
}

function getRefreshPromise(
    session: AuthSession,
): Promise<AuthSession> {
    if (!refreshPromise) {
        refreshPromise = refreshSession(session)
            .catch((error: unknown) => {
                const normalizedError =
                    normalizeApiError(error)

                if (
                    normalizedError.status === 401 ||
                    normalizedError.status === 403
                ) {
                    authStorage.clear()
                }

                throw normalizedError
            })
            .finally(() => {
                refreshPromise = null
            })
    }

    return refreshPromise
}

async function retryAfterUnauthorized(
    error: AxiosError,
): Promise<AxiosResponse> {
    const request =
        error.config as
        | RetryableRequestConfig
        | undefined

    if (
        error.response?.status !== 401 ||
        !request ||
        request.retriedAfterUnauthorized
    ) {
        throw error
    }

    const session = authStorage.read()

    if (!session) {
        throw error
    }

    request.retriedAfterUnauthorized = true

    const refreshedSession =
        await getRefreshPromise(session)

    request.headers.set(
        'Authorization',
        `${refreshedSession.tokenType} ${refreshedSession.accessToken}`,
    )

    return httpClient(request)
}

httpClient.interceptors.response.use(
    (response) => response,
    retryAfterUnauthorized,
)

function configureErrorHandling(
    client: AxiosInstance,
): void {
    client.interceptors.response.use(
        (response) => response,
        (error: unknown) =>
            Promise.reject(normalizeApiError(error)),
    )
}

configureErrorHandling(publicHttpClient)
configureErrorHandling(httpClient)
