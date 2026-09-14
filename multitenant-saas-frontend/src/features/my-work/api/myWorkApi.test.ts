import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { MyWorkOverview } from '../types/myWork'
import { myWorkApi } from './myWorkApi'

const overview: MyWorkOverview = {
    generatedAt: '2026-09-14T12:00:00Z',
    dueSoonHours: 72,
    summary: {
        totalOpen: 1,
        overdue: 1,
        dueSoon: 0,
        blocked: 0,
        inProgress: 0,
    },
    items: [],
}

describe('myWorkApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads the current user work queue with a bounded limit', async () => {
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue({
            data: {
                success: true,
                message: 'My Work fetched successfully',
                data: overview,
                timestamp: '2026-09-14T12:00:00Z',
            },
        })

        await expect(myWorkApi.getOverview('tenant-1', 75)).resolves.toEqual(overview)
        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/my-work', {
            params: { limit: 75 },
        })
    })
})
