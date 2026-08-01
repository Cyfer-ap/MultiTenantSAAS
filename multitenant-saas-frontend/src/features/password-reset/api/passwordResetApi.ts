import { publicHttpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    ForgotPasswordInput,
    ForgotPasswordResponse,
    ResetPasswordInput,
    ResetPasswordResponse,
} from '../types/passwordReset'

async function forgotPassword(
    tenantId: string,
    input: ForgotPasswordInput,
): Promise<ForgotPasswordResponse> {
    const response = await publicHttpClient.post<
        ApiResponse<ForgotPasswordResponse>
    >(
        `/api/tenants/${tenantId}/auth/forgot-password`,
        input,
    )

    return response.data.data
}

async function resetPassword(
    input: ResetPasswordInput,
): Promise<ResetPasswordResponse> {
    const response = await publicHttpClient.post<
        ApiResponse<ResetPasswordResponse>
    >('/api/auth/reset-password', input)

    return response.data.data
}

export const passwordResetApi = {
    forgotPassword,
    resetPassword,
}
