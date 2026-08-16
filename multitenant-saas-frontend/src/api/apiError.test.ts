import { AxiosError, AxiosHeaders } from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'

import type { ApiErrorResponse } from '../types/api'
import { normalizeApiError } from './apiError'

function createServerError(
    data: ApiErrorResponse | string,
    requestId: string,
): AxiosError<ApiErrorResponse | string> {
    const config: InternalAxiosRequestConfig = {
        headers: new AxiosHeaders(),
    }

    const headers = new AxiosHeaders()
    headers.set('x-request-id', requestId)

    const response: AxiosResponse<ApiErrorResponse | string> = {
        data,
        status: 500,
        statusText: 'Internal Server Error',
        headers,
        config,
    }

    return new AxiosError(
        'Request failed with status code 500',
        AxiosError.ERR_BAD_RESPONSE,
        config,
        undefined,
        response,
    )
}

describe('normalizeApiError', () => {
    it('preserves the backend request id for API errors', () => {
        const error = createServerError(
            {
                success: false,
                message: 'An unexpected error occurred.',
                errorCode: 'INTERNAL_SERVER_ERROR',
                status: 500,
                path: '/api/test',
                details: null,
                timestamp: new Date().toISOString(),
            },
            'request-123',
        )

        const normalized = normalizeApiError(error)

        expect(normalized.requestId).toBe('request-123')
        expect(normalized.status).toBe(500)
        expect(normalized.errorCode).toBe('INTERNAL_SERVER_ERROR')
    })

    it('preserves the request id for unexpected server responses', () => {
        const error = createServerError('unexpected-response', 'request-456')

        const normalized = normalizeApiError(error)

        expect(normalized.requestId).toBe('request-456')
        expect(normalized.status).toBe(500)
        expect(normalized.message).toBe('The server returned an unexpected response.')
    })
})
