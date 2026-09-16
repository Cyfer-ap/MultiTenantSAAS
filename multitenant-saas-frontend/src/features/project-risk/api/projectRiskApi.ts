import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { ProjectRiskResponse } from '../types/projectRisk'

async function getProjectRisk(tenantId: string, projectId: string): Promise<ProjectRiskResponse> {
    const response = await httpClient.get<ApiResponse<ProjectRiskResponse>>(
        `/api/tenants/${tenantId}/projects/${projectId}/risk`,
    )

    return response.data.data
}

export const projectRiskApi = {
    getProjectRisk,
}
