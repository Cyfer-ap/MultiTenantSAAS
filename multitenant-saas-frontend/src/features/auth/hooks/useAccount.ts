import {
    useMutation,
    useQuery,
} from '@tanstack/react-query'

import { authApi } from '../api/authApi'
import type { ChangePasswordInput } from '../types/auth'

export const currentUserQueryKey = [
    'auth',
    'current-user',
] as const

export function useCurrentUser() {
    return useQuery({
        queryKey: currentUserQueryKey,
        queryFn: authApi.getCurrentUser,
    })
}

export function useChangePassword() {
    return useMutation({
        mutationFn: (input: ChangePasswordInput) =>
            authApi.changePassword(input),
    })
}

export function useLogoutAllDevices() {
    return useMutation({
        mutationFn: authApi.logoutAllDevices,
    })
}
