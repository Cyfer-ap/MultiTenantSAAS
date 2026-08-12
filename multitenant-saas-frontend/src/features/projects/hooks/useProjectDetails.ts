import { useQuery } from '@tanstack/react-query'

import { projectsApi } from '../api/projectsApi'
import { tenantProjectsQueryKeys } from './useTenantProjects'

export function useProjectDetails(tenantId: string, projectId: string) {
    return useQuery({
        queryKey: tenantProjectsQueryKeys.detail(tenantId, projectId),
        queryFn: () => projectsApi.getProject(tenantId, projectId),
        enabled: tenantId.length > 0 && projectId.length > 0,
    })
}
