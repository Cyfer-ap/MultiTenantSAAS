export interface ForgotPasswordMutationInput {
    tenantId: string
    email: string
    workspaceGrantId: string
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
