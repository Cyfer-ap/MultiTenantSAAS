import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { savedViewsApi } from './savedViewsApi'

const savedView = {
    id: 'view-1',
    name: 'Urgent work',
    target: 'MY_WORK' as const,
    contextId: null,
    definition: { priority: 'URGENT' },
    createdAt: '2026-09-14T12:00:00Z',
    updatedAt: '2026-09-14T12:00:00Z',
}

describe('savedViewsApi', () => {
    beforeEach(() => vi.restoreAllMocks())

    it('lists a bounded surface scope without inventing a context', async () => {
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue({
            data: { success: true, message: 'ok', data: [savedView], timestamp: 'now' },
        })

        await expect(savedViewsApi.list('tenant-1', 'MY_WORK', null)).resolves.toEqual([
            savedView,
        ])
        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/saved-views', {
            params: { target: 'MY_WORK' },
        })
    })

    it('creates, updates and deletes views through user-owned endpoints', async () => {
        const post = vi.spyOn(httpClient, 'post').mockResolvedValue({
            data: { success: true, message: 'ok', data: savedView, timestamp: 'now' },
        })
        const put = vi.spyOn(httpClient, 'put').mockResolvedValue({
            data: { success: true, message: 'ok', data: savedView, timestamp: 'now' },
        })
        const remove = vi.spyOn(httpClient, 'delete').mockResolvedValue({ data: {} })

        await savedViewsApi.create('tenant-1', {
            name: 'Urgent work',
            target: 'MY_WORK',
            contextId: null,
            definition: { priority: 'URGENT' },
        })
        await savedViewsApi.update('tenant-1', 'view-1', {
            name: 'Urgent work',
            definition: { priority: 'URGENT' },
        })
        await savedViewsApi.remove('tenant-1', 'view-1')

        expect(post).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/saved-views',
            expect.any(Object),
        )
        expect(put).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/saved-views/view-1',
            expect.any(Object),
        )
        expect(remove).toHaveBeenCalledWith('/api/tenants/tenant-1/saved-views/view-1')
    })
})
