import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    Notification,
    NotificationMarkAllReadResult,
    NotificationUnreadCount,
} from '../types/notifications'

function notificationsPath(tenantId: string): string {
    return `/api/tenants/${tenantId}/notifications`
}

async function getNotifications(
    tenantId: string,
    page = 0,
    size = 10,
): Promise<PageResponse<Notification>> {
    const response = await httpClient.get<ApiResponse<PageResponse<Notification>>>(
        notificationsPath(tenantId),
        { params: { page, size } },
    )

    return response.data.data
}

async function getUnreadCount(tenantId: string): Promise<NotificationUnreadCount> {
    const response = await httpClient.get<ApiResponse<NotificationUnreadCount>>(
        `${notificationsPath(tenantId)}/unread-count`,
    )

    return response.data.data
}

async function markRead(tenantId: string, notificationId: string): Promise<Notification> {
    const response = await httpClient.patch<ApiResponse<Notification>>(
        `${notificationsPath(tenantId)}/${notificationId}/read`,
    )

    return response.data.data
}

async function markAllRead(tenantId: string): Promise<NotificationMarkAllReadResult> {
    const response = await httpClient.patch<ApiResponse<NotificationMarkAllReadResult>>(
        `${notificationsPath(tenantId)}/read-all`,
    )

    return response.data.data
}

export const notificationsApi = {
    getNotifications,
    getUnreadCount,
    markRead,
    markAllRead,
}
