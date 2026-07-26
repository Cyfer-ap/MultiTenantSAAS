export interface ApiResponse<T> {
    success: true
    message: string
    data: T
    timestamp: string
}

export interface PageResponse<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}

export type ErrorCode =
    | 'VALIDATION_FAILED'
    | 'INVALID_REQUEST'
    | 'INVALID_PARAMETER'
    | 'MALFORMED_REQUEST'
    | 'RESOURCE_NOT_FOUND'
    | 'RESOURCE_ALREADY_EXISTS'
    | 'AUTHENTICATION_REQUIRED'
    | 'AUTHENTICATION_FAILED'
    | 'ACCESS_DENIED'
    | 'INTERNAL_SERVER_ERROR'

export interface ApiErrorResponse {
    success: false
    message: string
    errorCode: ErrorCode
    status: number
    path: string
    details: Record<string, string> | null
    timestamp: string
}