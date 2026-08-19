import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import type { PageResponse } from '../../../types/api'
import { notificationsApi } from '../api/notificationsApi'
import type { Notification } from '../types/notifications'
import { NotificationCenter } from './NotificationCenter'

const notification: Notification = {
    id: 'notification-1',
    tenantId: 'tenant-1',
    recipientUserId: 'user-1',
    type: 'TASK_ASSIGNED',
    title: 'You were assigned a task',
    body: 'Ada Admin assigned "Review access controls" to you.',
    targetUrl: '/projects/project-1?task=task-1',
    readAt: null,
    createdAt: '2026-08-19T10:00:00Z',
}

const page: PageResponse<Notification> = {
    content: [notification],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

function LocationProbe() {
    const location = useLocation()
    return <output aria-label="Current location">{location.pathname + location.search}</output>
}

function renderCenter() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })

    return render(
        <ThemeProvider theme={appTheme}>
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/dashboard']}>
                    <NotificationCenter tenantId="tenant-1" />
                    <LocationProbe />
                </MemoryRouter>
            </QueryClientProvider>
        </ThemeProvider>,
    )
}

describe('NotificationCenter', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(notificationsApi, 'getUnreadCount').mockResolvedValue({ unreadCount: 1 })
        vi.spyOn(notificationsApi, 'getNotifications').mockResolvedValue(page)
        vi.spyOn(notificationsApi, 'markRead').mockResolvedValue({
            ...notification,
            readAt: '2026-08-19T10:05:00Z',
        })
        vi.spyOn(notificationsApi, 'markAllRead').mockResolvedValue({ markedReadCount: 1 })
    })

    it('shows unread notifications and opens their deep link after marking them read', async () => {
        const user = userEvent.setup()
        const markRead = vi.spyOn(notificationsApi, 'markRead')
        const popState = vi.fn()
        window.addEventListener('popstate', popState)
        renderCenter()

        const notificationButton = screen.getByRole('button', { name: /^notifications/i })
        await waitFor(
            () => {
                expect(notificationButton).toHaveAccessibleName(/notifications, 1 unread/i)
            },
            { timeout: 5_000 },
        )
        await user.click(notificationButton)
        await user.click(await screen.findByRole('button', { name: /you were assigned a task/i }))

        await waitFor(() => {
            expect(markRead).toHaveBeenCalledWith('tenant-1', 'notification-1')
        })
        await waitFor(() => {
            expect(screen.getByLabelText('Current location')).toHaveTextContent(
                '/projects/project-1?task=task-1',
            )
        })
        expect(popState).toHaveBeenCalledTimes(1)
        window.removeEventListener('popstate', popState)
    })

    it('marks every visible unread notification as read', async () => {
        const user = userEvent.setup()
        const markAllRead = vi.spyOn(notificationsApi, 'markAllRead')
        renderCenter()

        await user.click(screen.getByRole('button', { name: /^notifications/i }))
        await user.click(await screen.findByRole('button', { name: /mark all read/i }))

        await waitFor(() => {
            expect(markAllRead).toHaveBeenCalledWith('tenant-1')
        })
    })
})
