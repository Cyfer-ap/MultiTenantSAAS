import { publicHttpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    TenantOnboardingInput,
    TenantOnboardingResponse,
} from '../types/onboarding'

async function onboardTenant(
    input: TenantOnboardingInput,
): Promise<TenantOnboardingResponse> {
    const response = await publicHttpClient.post<
        ApiResponse<TenantOnboardingResponse>
    >('/api/onboarding/tenants', input)

    return response.data.data
}

export const onboardingApi = {
    onboardTenant,
}
