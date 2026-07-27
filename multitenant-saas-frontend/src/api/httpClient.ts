import axios from 'axios'
import type { AxiosInstance } from 'axios'

import { env } from '../config/env'
import { authStorage } from '../features/auth/storage/authStorage'
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