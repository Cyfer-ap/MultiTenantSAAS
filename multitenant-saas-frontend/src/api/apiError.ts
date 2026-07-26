import axios from 'axios'

import type {
    ApiErrorResponse,
    ErrorCode,
} from '../types/api'

interface ApiClientErrorOptions {
    message: string
    errorCode?: ErrorCode
    status?: number
    path?: string
    details?: Record<string, string> | null
    timestamp?: string
    networkError?: boolean
}

export class ApiClientError extends Error {
    readonly errorCode?: ErrorCode
    readonly status?: number
    readonly path?: string
    readonly details: Record<string, string> | null
    readonly timestamp?: string
    readonly networkError: boolean

    constructor({
                    message,
                    errorCode,
                    status,
                    path,
                    details = null,
                    timestamp,
                    networkError = false,
                }: ApiClientErrorOptions) {
        super(message)

        this.name = 'ApiClientError'
        this.errorCode = errorCode
        this.status = status
        this.path = path
        this.details = details
        this.timestamp = timestamp
        this.networkError = networkError
    }
}

function isApiErrorResponse(
    value: unknown,
): value is ApiErrorResponse {
    if (
        typeof value !== 'object' ||
        value === null
    ) {
        return false
    }

    const candidate =
        value as Partial<ApiErrorResponse>

    return (
        candidate.success === false &&
        typeof candidate.message === 'string' &&
        typeof candidate.errorCode === 'string' &&
        typeof candidate.status === 'number'
    )
}

export function normalizeApiError(
    error: unknown,
): ApiClientError {
    if (error instanceof ApiClientError) {
        return error
    }

    if (axios.isAxiosError(error)) {
        const responseBody: unknown =
            error.response?.data

        if (isApiErrorResponse(responseBody)) {
            return new ApiClientError({
                message: responseBody.message,
                errorCode: responseBody.errorCode,
                status: responseBody.status,
                path: responseBody.path,
                details: responseBody.details,
                timestamp: responseBody.timestamp,
            })
        }

        if (!error.response) {
            return new ApiClientError({
                message:
                    'Unable to connect to the server. Check your network connection and ensure the backend is running.',
                networkError: true,
            })
        }

        return new ApiClientError({
            message:
                'The server returned an unexpected response.',
            status: error.response.status,
        })
    }

    if (error instanceof Error) {
        return new ApiClientError({
            message: error.message,
        })
    }

    return new ApiClientError({
        message: 'An unexpected error occurred.',
    })
}