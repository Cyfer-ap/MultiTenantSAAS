import { publicHttpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    ForgotPasswordMutationInput,
    ForgotPasswordResponse,
    ResetPasswordInput,
    ResetPasswordResponse,
} from '../types/passwordReset'

async function forgotPassword(input: ForgotPasswordMutationInput): Promise<ForgotPasswordResponse> {
    const response = await publicHttpClient.post<ApiResponse<ForgotPasswordResponse>>(
        '/api/auth/password-reset/request',
        input,
    )

    return response.data.data
}

async function resetPassword(input: ResetPasswordInput): Promise<ResetPasswordResponse> {
    const response = await publicHttpClient.post<ApiResponse<ResetPasswordResponse>>(
        '/api/auth/password-reset/complete',
        input,
    )

    return response.data.data
}

export const passwordResetApi = {
    forgotPassword,
    resetPassword,
}
