import {
    keepPreviousData,
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import { systemAdminApi } from '../api/systemAdminApi'
import type {
    SystemTenantOnboardingInput,
    SystemTenantsQueryParams,
    UpdateSystemTenantStatusInput,
} from '../types/systemAdmin'
import { systemDashboardQueryKey } from './useSystemDashboard'

export const systemTenantsQueryKeys = {
    all: ['system-admin', 'tenants'] as const,
    list: (params: SystemTenantsQueryParams) => [
        ...systemTenantsQueryKeys.all,
        params,
    ] as const,
}

export function useSystemTenants(
    params: SystemTenantsQueryParams,
) {
    return useQuery({
        queryKey: systemTenantsQueryKeys.list(params),
        queryFn: () => systemAdminApi.getTenants(params),
        placeholderData: keepPreviousData,
    })
}

export function useUpdateSystemTenantStatus() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({
            tenantId,
            input,
        }: {
            tenantId: string
            input: UpdateSystemTenantStatusInput
        }) =>
            systemAdminApi.updateTenantStatus(
                tenantId,
                input,
            ),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: systemTenantsQueryKeys.all,
                }),
                queryClient.invalidateQueries({
                    queryKey: systemDashboardQueryKey,
                }),
            ])
        },
    })
}

export function useSystemTenantOnboarding() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (
            input: SystemTenantOnboardingInput,
        ) => systemAdminApi.onboardTenant(input),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: systemTenantsQueryKeys.all,
                }),
                queryClient.invalidateQueries({
                    queryKey: systemDashboardQueryKey,
                }),
            ])
        },
    })
}
