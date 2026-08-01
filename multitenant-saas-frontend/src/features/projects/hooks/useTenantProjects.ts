import {
    keepPreviousData,
    useQuery,
} from '@tanstack/react-query'

import { projectsApi } from '../api/projectsApi'
import type { TenantProjectsQueryParams } from '../types/projects'

export const tenantProjectsQueryKeys = {
    all: ['tenant-projects'] as const,
    tenant: (tenantId: string) => [
        ...tenantProjectsQueryKeys.all,
        tenantId,
    ] as const,
    detail: (tenantId: string, projectId: string) => [
        ...tenantProjectsQueryKeys.tenant(tenantId),
        'detail',
        projectId,
    ] as const,
    list: (
        tenantId: string,
        params: TenantProjectsQueryParams,
    ) => [
        ...tenantProjectsQueryKeys.tenant(tenantId),
        params,
    ] as const,
}

export function useTenantProjects(
    tenantId: string,
    params: TenantProjectsQueryParams,
) {
    return useQuery({
        queryKey: tenantProjectsQueryKeys.list(
            tenantId,
            params,
        ),
        queryFn: () =>
            projectsApi.getProjects(tenantId, params),
        enabled: tenantId.length > 0,
        placeholderData: keepPreviousData,
    })
}
