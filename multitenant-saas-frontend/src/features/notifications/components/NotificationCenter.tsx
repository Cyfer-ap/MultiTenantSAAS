import NotificationsNoneRoundedIcon from '@mui/icons-material/NotificationsNoneRounded'
import {
    Badge,
    Box,
    Button,
    CircularProgress,
    Divider,
    IconButton,
    List,
    ListItemButton,
    Popover,
    Stack,
    Typography,
} from '@mui/material'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import {
    useMarkAllNotificationsRead,
    useMarkNotificationRead,
    useNotifications,
    useNotificationUnreadCount,
} from '../hooks/useNotifications'
import type { Notification } from '../types/notifications'

interface NotificationCenterProps {
    tenantId: string
}

function formatTimestamp(value: string): string {
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(new Date(value))
}

export function NotificationCenter({ tenantId }: NotificationCenterProps) {
    const navigate = useNavigate()
    const [anchorElement, setAnchorElement] = useState<HTMLElement | null>(null)
    const open = Boolean(anchorElement)
    const notificationsQuery = useNotifications(tenantId, open)
    const unreadCountQuery = useNotificationUnreadCount(tenantId)
    const markReadMutation = useMarkNotificationRead(tenantId)
    const markAllReadMutation = useMarkAllNotificationsRead(tenantId)

    const unreadCount = unreadCountQuery.data?.unreadCount ?? 0
    const notifications = notificationsQuery.data?.content ?? []

    async function selectNotification(notification: Notification) {
        if (!notification.readAt) {
            await markReadMutation.mutateAsync(notification.id)
        }

        setAnchorElement(null)
        if (notification.targetUrl?.startsWith('/') && !notification.targetUrl.startsWith('//')) {
            navigate(notification.targetUrl)
            window.dispatchEvent(new PopStateEvent('popstate'))
        }
    }

    return (
        <>
            <IconButton
                aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ''}`}
                onClick={(event) => setAnchorElement(event.currentTarget)}
                size="small"
            >
                <Badge badgeContent={unreadCount} color="error" max={99}>
                    <NotificationsNoneRoundedIcon />
                </Badge>
            </IconButton>

            <Popover
                anchorEl={anchorElement}
                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                onClose={() => setAnchorElement(null)}
                open={open}
                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                slotProps={{
                    paper: {
                        sx: { mt: 1, width: { xs: 340, sm: 400 }, maxWidth: 'calc(100vw - 24px)' },
                    },
                }}
            >
                <Stack direction="row" sx={{ alignItems: 'center', px: 2, py: 1.5 }}>
                    <Box sx={{ flexGrow: 1 }}>
                        <Typography sx={{ fontWeight: 750 }}>Notifications</Typography>
                        <Typography color="text.secondary" variant="caption">
                            {unreadCount > 0 ? `${unreadCount} unread` : 'You are all caught up'}
                        </Typography>
                    </Box>
                    {unreadCount > 0 && (
                        <Button
                            disabled={markAllReadMutation.isPending}
                            onClick={() => markAllReadMutation.mutate()}
                            size="small"
                        >
                            Mark all read
                        </Button>
                    )}
                </Stack>
                <Divider />

                {notificationsQuery.isPending && (
                    <Stack sx={{ alignItems: 'center', p: 4 }}>
                        <CircularProgress aria-label="Loading notifications" size={28} />
                    </Stack>
                )}

                {notificationsQuery.isError && (
                    <Stack spacing={1.5} sx={{ alignItems: 'center', p: 3, textAlign: 'center' }}>
                        <Typography color="error">Notifications could not be loaded.</Typography>
                        <Button onClick={() => notificationsQuery.refetch()} size="small">
                            Try again
                        </Button>
                    </Stack>
                )}

                {notificationsQuery.isSuccess && notifications.length === 0 && (
                    <Stack sx={{ alignItems: 'center', p: 4, textAlign: 'center' }}>
                        <Typography sx={{ fontWeight: 650 }}>No notifications yet</Typography>
                        <Typography color="text.secondary" variant="body2">
                            Task assignments and other workspace updates will appear here.
                        </Typography>
                    </Stack>
                )}

                {notificationsQuery.isSuccess && notifications.length > 0 && (
                    <List disablePadding sx={{ maxHeight: 440, overflowY: 'auto' }}>
                        {notifications.map((notification, index) => (
                            <Box key={notification.id}>
                                {index > 0 && <Divider />}
                                <ListItemButton
                                    onClick={() => void selectNotification(notification)}
                                    sx={{ alignItems: 'flex-start', gap: 1.25, px: 2, py: 1.5 }}
                                >
                                    <Box
                                        aria-hidden="true"
                                        sx={{
                                            bgcolor: notification.readAt
                                                ? 'transparent'
                                                : 'primary.main',
                                            borderRadius: '50%',
                                            flexShrink: 0,
                                            height: 8,
                                            mt: 0.8,
                                            width: 8,
                                        }}
                                    />
                                    <Box sx={{ minWidth: 0 }}>
                                        <Typography
                                            sx={{
                                                fontWeight: notification.readAt ? 600 : 800,
                                                lineHeight: 1.35,
                                            }}
                                            variant="body2"
                                        >
                                            {notification.title}
                                        </Typography>
                                        <Typography color="text.secondary" variant="body2">
                                            {notification.body}
                                        </Typography>
                                        <Typography color="text.secondary" variant="caption">
                                            {formatTimestamp(notification.createdAt)}
                                        </Typography>
                                    </Box>
                                </ListItemButton>
                            </Box>
                        ))}
                    </List>
                )}
            </Popover>
        </>
    )
}
