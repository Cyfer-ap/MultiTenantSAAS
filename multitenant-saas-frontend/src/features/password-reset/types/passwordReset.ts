export interface ForgotPasswordInput {
    email: string
}

export interface ForgotPasswordMutationInput extends ForgotPasswordInput {
    tenantId: string
}

export interface ForgotPasswordResponse {
    message: string
    devResetToken: string | null
}

export interface ResetPasswordInput {
    resetToken: string
    newPassword: string
    confirmPassword: string
}

export interface ResetPasswordResponse {
    message: string
}
