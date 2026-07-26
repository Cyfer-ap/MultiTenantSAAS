import axios from 'axios'

import { env } from '../config/env'
import { normalizeApiError } from './apiError'

export const httpClient = axios.create({
    baseURL: env.apiBaseUrl,
    timeout: 15_000,
    headers: {
        Accept: 'application/json',
    },
})

httpClient.interceptors.response.use(
    (response) => response,
    (error: unknown) =>
        Promise.reject(normalizeApiError(error)),
)