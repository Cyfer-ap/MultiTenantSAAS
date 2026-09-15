import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    ProjectSimulationBaseline,
    ProjectSimulationRequest,
    ProjectSimulationResponse,
} from '../types/projectSimulation'

async function getBaseline(
    tenantId: string,
    projectId: string,
): Promise<ProjectSimulationBaseline> {
    const response = await httpClient.get<ApiResponse<ProjectSimulationBaseline>>(
        `/api/tenants/${tenantId}/projects/${projectId}/simulation/baseline`,
    )

    return response.data.data
}

async function simulate(
    tenantId: string,
    projectId: string,
    input: ProjectSimulationRequest,
): Promise<ProjectSimulationResponse> {
    const response = await httpClient.post<ApiResponse<ProjectSimulationResponse>>(
        `/api/tenants/${tenantId}/projects/${projectId}/simulation`,
        input,
    )

    return response.data.data
}

export const projectSimulationApi = {
    getBaseline,
    simulate,
}
