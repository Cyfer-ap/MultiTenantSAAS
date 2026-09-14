import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { CalendarPage } from './CalendarPage'

const { useCalendarDeadlinesMock } = vi.hoisted(() => ({
    useCalendarDeadlinesMock: vi.fn(),
}))

vi.mock('../../auth/hooks/useAuth', () => ({
    useAuth: () => ({ session: { tenantId: 'tenant-1' } }),
}))

vi.mock('../hooks/useCalendarDeadlines', () => ({
    useCalendarDeadlines: useCalendarDeadlinesMock,
}))

function renderPage() {
    return render(
        <ThemeProvider theme={appTheme}>
            <MemoryRouter>
                <CalendarPage />
            </MemoryRouter>
        </ThemeProvider>,
    )
}

function deadlineResponse(overrides: Record<string, unknown> = {}) {
    const dueAt = new Date()
    dueAt.setHours(10, 30, 0, 0)

    return {
        generatedAt: new Date().toISOString(),
        from: new Date().toISOString(),
        to: new Date().toISOString(),
        returnedCount: 1,
        truncated: false,
        items: [
            {
                taskId: 'task-1',
                projectId: 'project-1',
                title: 'Prepare release',
                projectName: 'Phoenix',
                status: 'IN_PROGRESS',
                priority: 'HIGH',
                dueAt: dueAt.toISOString(),
                targetUrl: '/projects/project-1?task=task-1',
            },
        ],
        ...overrides,
    }
}

describe('CalendarPage', () => {
    beforeEach(() => {
        useCalendarDeadlinesMock.mockReset()
        useCalendarDeadlinesMock.mockReturnValue({
            data: deadlineResponse(),
            isPending: false,
            isError: false,
            refetch: vi.fn(),
        })
    })

    it('renders accessible deadlines and owning task deep links', () => {
        renderPage()

        expect(screen.getByRole('heading', { name: 'Calendar' })).toBeInTheDocument()
        expect(screen.getAllByText(/Prepare release/).length).toBeGreaterThan(0)
        expect(screen.getAllByText('Phoenix').length).toBeGreaterThan(0)
        expect(screen.getByRole('link', { name: /open task/i })).toHaveAttribute(
            'href',
            '/projects/project-1?task=task-1',
        )
    })

    it('requests a new bounded range when moving to the next month', async () => {
        const user = userEvent.setup()
        renderPage()
        const firstCall = useCalendarDeadlinesMock.mock.calls.at(-1)

        await user.click(screen.getByRole('button', { name: /next month/i }))

        const latestCall = useCalendarDeadlinesMock.mock.calls.at(-1)
        expect(latestCall?.[0]).toBe('tenant-1')
        expect(latestCall?.[1]).not.toBe(firstCall?.[1])
        expect(latestCall?.[2]).not.toBe(firstCall?.[2])
        expect(latestCall?.[3]).toBe(500)
    })

    it('warns when the bounded calendar result is truncated', () => {
        useCalendarDeadlinesMock.mockReturnValue({
            data: deadlineResponse({ truncated: true }),
            isPending: false,
            isError: false,
            refetch: vi.fn(),
        })

        renderPage()

        expect(screen.getByText(/more than 500 accessible deadlines/i)).toBeInTheDocument()
    })
})
