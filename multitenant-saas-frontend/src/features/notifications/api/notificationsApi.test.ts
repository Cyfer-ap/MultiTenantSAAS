import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { Notification } from '../types/notifications'
import { notificationsApi } from './notificationsApi'

const notification: Notification = {
    id: 'notification-1',
    tenantId: 'tenant-1',
    recipientUserId: 'user-1',
    type: 'TASK_ASSIGNED',
    title: 'You were assigned a task',
    body: 'Ada assigned a task to you.',
    targetUrl: '/projects/project-1?task=task-1',
    readAt: null,
    createdAt: '2026-08-19T10:00:00Z',
}

function successfulResponse(data: unknown) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-19T10:00:00Z',
        },
    }
}

describe('notificationsApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('uses recipient-scoped list and unread count endpoints', async () => {
        const page = {
            content: [notification],
            page: 0,
            size: 10,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
        }
        const get = vi
            .spyOn(httpClient, 'get')
            .mockResolvedValueOnce(successfulResponse(page))
            .mockResolvedValueOnce(successfulResponse({ unreadCount: 1 }))

        await expect(notificationsApi.getNotifications('tenant-1')).resolves.toEqual(page)
        await expect(notificationsApi.getUnreadCount('tenant-1')).resolves.toEqual({
            unreadCount: 1,
        })

        expect(get).toHaveBeenNthCalledWith(1, '/api/tenants/tenant-1/notifications', {
            params: { page: 0, size: 10 },
        })
        expect(get).toHaveBeenNthCalledWith(2, '/api/tenants/tenant-1/notifications/unread-count')
    })

    it('uses the exact read mutation endpoints', async () => {
        const patch = vi
            .spyOn(httpClient, 'patch')
            .mockResolvedValueOnce(
                successfulResponse({ ...notification, readAt: '2026-08-19T10:05:00Z' }),
            )
            .mockResolvedValueOnce(successfulResponse({ markedReadCount: 3 }))

        await notificationsApi.markRead('tenant-1', 'notification-1')
        await notificationsApi.markAllRead('tenant-1')

        expect(patch).toHaveBeenNthCalledWith(
            1,
            '/api/tenants/tenant-1/notifications/notification-1/read',
        )
        expect(patch).toHaveBeenNthCalledWith(2, '/api/tenants/tenant-1/notifications/read-all')
    })
})
