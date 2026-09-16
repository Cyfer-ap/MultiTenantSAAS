import { useQuery } from '@tanstack/react-query'

import { projectRiskApi } from '../api/projectRiskApi'

export const projectRiskQueryKeys = {
    all: ['project-risk'] as const,
    detail: (tenantId: string, projectId: string) =>
        [...projectRiskQueryKeys.all, tenantId, projectId] as const,
}

export function useProjectRisk(tenantId: string, projectId: string) {
    return useQuery({
        queryKey: projectRiskQueryKeys.detail(tenantId, projectId),
        queryFn: () => projectRiskApi.getProjectRisk(tenantId, projectId),
        enabled: tenantId.length > 0 && projectId.length > 0,
    })
}
