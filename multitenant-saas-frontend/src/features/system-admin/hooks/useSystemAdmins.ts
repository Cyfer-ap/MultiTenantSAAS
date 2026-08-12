import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { systemAdminApi } from '../api/systemAdminApi'
import type {
    CreateSystemAdminInput,
    SystemAdminsQueryParams,
    UpdateSystemAdminStatusInput,
} from '../types/systemAdmin'
import { platformAuditLogQueryKeys } from './usePlatformAuditLogs'

export const systemAdminsQueryKeys = {
    all: ['system-admin', 'accounts'] as const,
    list: (params: SystemAdminsQueryParams) => [...systemAdminsQueryKeys.all, params] as const,
}

export function useSystemAdmins(params: SystemAdminsQueryParams) {
    return useQuery({
        queryKey: systemAdminsQueryKeys.list(params),
        queryFn: () => systemAdminApi.getSystemAdmins(params),
        placeholderData: keepPreviousData,
    })
}

function useInvalidateSystemAdminManagement() {
    const queryClient = useQueryClient()

    return async (): Promise<void> => {
        await Promise.all([
            queryClient.invalidateQueries({
                queryKey: systemAdminsQueryKeys.all,
            }),
            queryClient.invalidateQueries({
                queryKey: platformAuditLogQueryKeys.all,
            }),
        ])
    }
}

export function useCreateSystemAdmin() {
    const invalidate = useInvalidateSystemAdminManagement()

    return useMutation({
        mutationFn: (input: CreateSystemAdminInput) => systemAdminApi.createSystemAdmin(input),
        onSuccess: invalidate,
    })
}

export function useUpdateSystemAdminStatus() {
    const invalidate = useInvalidateSystemAdminManagement()

    return useMutation({
        mutationFn: ({
            systemAdminId,
            input,
        }: {
            systemAdminId: string
            input: UpdateSystemAdminStatusInput
        }) => systemAdminApi.updateSystemAdminStatus(systemAdminId, input),
        onSuccess: invalidate,
    })
}

export function useUnlockSystemAdminLogin() {
    const invalidate = useInvalidateSystemAdminManagement()

    return useMutation({
        mutationFn: (systemAdminId: string) => systemAdminApi.unlockSystemAdminLogin(systemAdminId),
        onSuccess: invalidate,
    })
}
