import { publicHttpClient } from '../../../api/httpClient'
import { systemHttpClient } from '../../../api/systemHttpClient'
import type {
    ApiResponse,
    PageResponse,
} from '../../../types/api'
import type {
    CurrentSystemAdmin,
    CreateSystemAdminInput,
    PlatformAuditLog,
    PlatformAuditLogsQueryParams,
    SystemAdminRecord,
    SystemAdminsQueryParams,
    SystemAdminLoginInput,
    SystemAdminLoginResponse,
    SystemDashboardSummary,
    SystemTenant,
    SystemTenantOnboardingInput,
    SystemTenantOnboardingResponse,
    SystemTenantsQueryParams,
    UpdateSystemTenantStatusInput,
    UpdateSystemAdminStatusInput,
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

async function getSystemAdmins(
    params: SystemAdminsQueryParams,
): Promise<PageResponse<SystemAdminRecord>> {
    const response = await systemHttpClient.get<
        ApiResponse<PageResponse<SystemAdminRecord>>
    >('/api/system/admins', { params })

    return response.data.data
}

async function createSystemAdmin(
    input: CreateSystemAdminInput,
): Promise<SystemAdminRecord> {
    const response = await systemHttpClient.post<
        ApiResponse<SystemAdminRecord>
    >('/api/system/admins', input)

    return response.data.data
}

async function updateSystemAdminStatus(
    systemAdminId: string,
    input: UpdateSystemAdminStatusInput,
): Promise<SystemAdminRecord> {
    const response = await systemHttpClient.patch<
        ApiResponse<SystemAdminRecord>
    >(`/api/system/admins/${systemAdminId}/status`, input)

    return response.data.data
}

async function unlockSystemAdminLogin(
    systemAdminId: string,
): Promise<SystemAdminRecord> {
    const response = await systemHttpClient.patch<
        ApiResponse<SystemAdminRecord>
    >(`/api/system/admins/${systemAdminId}/unlock`)

    return response.data.data
}

async function getPlatformAuditLogs(
    params: PlatformAuditLogsQueryParams,
): Promise<PageResponse<PlatformAuditLog>> {
    const response = await systemHttpClient.get<
        ApiResponse<PageResponse<PlatformAuditLog>>
    >('/api/system/audit-logs', { params })

    return response.data.data
}

export const systemAdminApi = {
    login,
    getCurrentAdmin,
    getDashboardSummary,
    getTenants,
    updateTenantStatus,
    onboardTenant,
    getSystemAdmins,
    createSystemAdmin,
    updateSystemAdminStatus,
    unlockSystemAdminLogin,
    getPlatformAuditLogs,
}
