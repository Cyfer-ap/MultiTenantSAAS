import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { notificationsApi } from '../api/notificationsApi'

export const notificationQueryKeys = {
    all: ['notifications'] as const,
    tenant: (tenantId: string) => [...notificationQueryKeys.all, tenantId] as const,
    list: (tenantId: string) => [...notificationQueryKeys.tenant(tenantId), 'list'] as const,
    unreadCount: (tenantId: string) =>
        [...notificationQueryKeys.tenant(tenantId), 'unread-count'] as const,
}

export function useNotifications(tenantId: string, enabled = true) {
    return useQuery({
        queryKey: notificationQueryKeys.list(tenantId),
        queryFn: () => notificationsApi.getNotifications(tenantId),
        enabled: enabled && tenantId.length > 0,
    })
}

export function useNotificationUnreadCount(tenantId: string) {
    return useQuery({
        queryKey: notificationQueryKeys.unreadCount(tenantId),
        queryFn: () => notificationsApi.getUnreadCount(tenantId),
        enabled: tenantId.length > 0,
        refetchInterval: 30_000,
    })
}

function useInvalidateNotifications(tenantId: string) {
    const queryClient = useQueryClient()

    return async (): Promise<void> => {
        await queryClient.invalidateQueries({
            queryKey: notificationQueryKeys.tenant(tenantId),
        })
    }
}

export function useMarkNotificationRead(tenantId: string) {
    const invalidate = useInvalidateNotifications(tenantId)

    return useMutation({
        mutationFn: (notificationId: string) => notificationsApi.markRead(tenantId, notificationId),
        onSuccess: invalidate,
    })
}

export function useMarkAllNotificationsRead(tenantId: string) {
    const invalidate = useInvalidateNotifications(tenantId)

    return useMutation({
        mutationFn: () => notificationsApi.markAllRead(tenantId),
        onSuccess: invalidate,
    })
}
