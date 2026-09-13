import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { GlobalSearchResponse } from '../types/search'
import { globalSearchApi } from './globalSearchApi'

const searchResponse: GlobalSearchResponse = {
    query: 'phoenix',
    results: [
        {
            type: 'PROJECT',
            id: 'project-1',
            parentId: null,
            title: 'Phoenix',
            subtitle: 'ACTIVE',
        },
    ],
}

describe('globalSearchApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('sends a bounded tenant search query', async () => {
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue({
            data: {
                success: true,
                message: 'Search completed successfully',
                data: searchResponse,
                timestamp: '2026-09-14T00:00:00Z',
            },
        })

        await expect(globalSearchApi.search('tenant-1', 'phoenix', 12)).resolves.toEqual(
            searchResponse,
        )

        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/search', {
            params: {
                q: 'phoenix',
                limit: 12,
            },
        })
    })
})
