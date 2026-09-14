import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { PersonalWorkspaceOverview } from '../types/personalWorkspace'
import { personalWorkspaceApi } from './personalWorkspaceApi'

const overview: PersonalWorkspaceOverview = {
    favorites: [],
    recent: [],
}

describe('personalWorkspaceApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads the current user personal workspace with a bounded limit', async () => {
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue({
            data: {
                success: true,
                message: 'Personal workspace fetched successfully',
                data: overview,
                timestamp: '2026-09-14T00:00:00Z',
            },
        })

        await expect(personalWorkspaceApi.getOverview('tenant-1', 16)).resolves.toEqual(overview)
        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/personal-workspace', {
            params: { limit: 16 },
        })
    })

    it('uses resource-scoped endpoints for favorite, unfavorite and recent actions', async () => {
        const item = {
            type: 'PROJECT' as const,
            resourceId: 'project-1',
            parentId: null,
            title: 'Phoenix',
            subtitle: 'ACTIVE',
            favoriteAt: '2026-09-14T00:00:00Z',
            lastViewedAt: null,
        }
        const put = vi.spyOn(httpClient, 'put').mockResolvedValue({
            data: { success: true, message: 'ok', data: item, timestamp: 'now' },
        })
        const post = vi.spyOn(httpClient, 'post').mockResolvedValue({
            data: { success: true, message: 'ok', data: item, timestamp: 'now' },
        })
        const remove = vi.spyOn(httpClient, 'delete').mockResolvedValue({ data: {} })

        await personalWorkspaceApi.favorite('tenant-1', 'PROJECT', 'project-1')
        await personalWorkspaceApi.recordRecent('tenant-1', 'PROJECT', 'project-1')
        await personalWorkspaceApi.unfavorite('tenant-1', 'PROJECT', 'project-1')

        expect(put).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/personal-workspace/favorites/PROJECT/project-1',
        )
        expect(post).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/personal-workspace/recent/PROJECT/project-1',
        )
        expect(remove).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/personal-workspace/favorites/PROJECT/project-1',
        )
    })
})
