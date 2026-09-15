import { useMutation, useQuery } from '@tanstack/react-query'

import { projectSimulationApi } from '../api/projectSimulationApi'
import type { ProjectSimulationRequest } from '../types/projectSimulation'

export const projectSimulationQueryKeys = {
    all: ['project-simulation'] as const,
    baseline: (tenantId: string, projectId: string) =>
        [...projectSimulationQueryKeys.all, 'baseline', tenantId, projectId] as const,
}

export function useProjectSimulationBaseline(tenantId: string, projectId: string) {
    return useQuery({
        queryKey: projectSimulationQueryKeys.baseline(tenantId, projectId),
        queryFn: () => projectSimulationApi.getBaseline(tenantId, projectId),
        enabled: tenantId.length > 0 && projectId.length > 0,
    })
}

export function useRunProjectSimulation(tenantId: string, projectId: string) {
    return useMutation({
        mutationFn: (input: ProjectSimulationRequest) =>
            projectSimulationApi.simulate(tenantId, projectId, input),
    })
}
