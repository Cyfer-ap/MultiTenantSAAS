import { useMutation } from '@tanstack/react-query'

import { onboardingApi } from '../api/onboardingApi'
import type { TenantOnboardingInput } from '../types/onboarding'

export function useTenantOnboarding() {
    return useMutation({
        mutationFn: (input: TenantOnboardingInput) => onboardingApi.onboardTenant(input),
    })
}
