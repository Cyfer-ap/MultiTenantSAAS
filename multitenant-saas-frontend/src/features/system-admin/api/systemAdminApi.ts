import { publicHttpClient } from '../../../api/httpClient'
import { systemHttpClient } from '../../../api/systemHttpClient'
import type {
    ApiResponse,
    PageResponse,
} from '../../../types/api'
import type {
    CurrentSystemAdmin,
    SystemAdminLoginInput,
    SystemAdminLoginResponse,
    SystemDashboardSummary,
    SystemTenant,
    SystemTenantOnboardingInput,
    SystemTenantOnboardingResponse,
    SystemTenantsQueryParams,
    UpdateSystemTenantStatusInput,
} from '../types/systemAdmin'

async function login(
    input: SystemAdminLoginInput,
): Promise<SystemAdminLoginResponse> {
    const response = await publicHttpClient.post<
        ApiResponse<SystemAdminLoginResponse>
    >('/api/system/auth/login', input)

    return response.data.data
}

async function getCurrentAdmin(): Promise<CurrentSystemAdmin> {
    const response = await systemHttpClient.get<
        ApiResponse<CurrentSystemAdmin>
    >('/api/system/auth/me')

    return response.data.data
}

async function getDashboardSummary(): Promise<SystemDashboardSummary> {
    const response = await systemHttpClient.get<
        ApiResponse<SystemDashboardSummary>
    >('/api/dashboard/summary')

    return response.data.data
}

async function getTenants(
    params: SystemTenantsQueryParams,
): Promise<PageResponse<SystemTenant>> {
    const response = await systemHttpClient.get<
        ApiResponse<PageResponse<SystemTenant>>
    >('/api/tenants', { params })

    return response.data.data
}

async function updateTenantStatus(
    tenantId: string,
    input: UpdateSystemTenantStatusInput,
): Promise<SystemTenant> {
    const response = await systemHttpClient.patch<
        ApiResponse<SystemTenant>
    >(`/api/tenants/${tenantId}/status`, input)

    return response.data.data
}

async function onboardTenant(
    input: SystemTenantOnboardingInput,
): Promise<SystemTenantOnboardingResponse> {
    const response = await systemHttpClient.post<
        ApiResponse<SystemTenantOnboardingResponse>
    >('/api/system/onboarding/tenants', input)

    return response.data.data
}

export const systemAdminApi = {
    login,
    getCurrentAdmin,
    getDashboardSummary,
    getTenants,
    updateTenantStatus,
    onboardTenant,
}
