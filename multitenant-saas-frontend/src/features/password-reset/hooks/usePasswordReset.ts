import { useMutation } from '@tanstack/react-query'

import { passwordResetApi } from '../api/passwordResetApi'
import type { ForgotPasswordMutationInput, ResetPasswordInput } from '../types/passwordReset'

export function useForgotPassword() {
    return useMutation({
        mutationFn: (input: ForgotPasswordMutationInput) => passwordResetApi.forgotPassword(input),
    })
}

export function useResetPassword() {
    return useMutation({
        mutationFn: (input: ResetPasswordInput) => passwordResetApi.resetPassword(input),
    })
}
