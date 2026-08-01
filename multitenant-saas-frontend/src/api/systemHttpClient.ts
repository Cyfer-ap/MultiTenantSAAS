import axios from 'axios'

import { env } from '../config/env'
import { systemAdminStorage } from '../features/system-admin/storage/systemAdminStorage'
import { normalizeApiError } from './apiError'

export const systemHttpClient = axios.create({
    baseURL: env.apiBaseUrl,
    timeout: 15_000,
    headers: {
        Accept: 'application/json',
    },
})

systemHttpClient.interceptors.request.use(
    (config) => {
        const session = systemAdminStorage.read()

        if (session) {
            config.headers.set(
                'Authorization',
                `${session.tokenType} ${session.accessToken}`,
            )
        }

        return config
    },
)

systemHttpClient.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
        const normalizedError = normalizeApiError(error)

        if (
            normalizedError.status === 401 ||
            normalizedError.status === 403
        ) {
            systemAdminStorage.clear()
        }

        return Promise.reject(normalizedError)
    },
)
