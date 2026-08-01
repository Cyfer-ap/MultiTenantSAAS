import { useMutation } from '@tanstack/react-query'

import { systemAdminApi } from '../api/systemAdminApi'
import type { ChangeSystemAdminPasswordInput } from '../types/systemAdmin'

export function useChangeSystemAdminPassword() {
    return useMutation({
        mutationFn: (input: ChangeSystemAdminPasswordInput) =>
            systemAdminApi.changePassword(input),
    })
}
