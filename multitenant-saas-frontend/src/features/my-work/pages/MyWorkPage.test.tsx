import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import { MyWorkPage } from './MyWorkPage'

vi.mock('../../auth/hooks/useAuth', () => ({
    useAuth: () => ({ session: { tenantId: 'tenant-1' } }),
}))

vi.mock('../hooks/useMyWork', () => ({
    useMyWork: () => ({
        data: {
            generatedAt: '2026-09-14T12:00:00Z',
            dueSoonHours: 72,
            summary: {
                totalOpen: 2,
                overdue: 1,
                dueSoon: 0,
                blocked: 1,
                inProgress: 0,
            },
            items: [
                {
                    taskId: 'task-1',
                    projectId: 'project-1',
                    title: 'Repair billing cancellation',
                    projectName: 'Platform',
                    status: 'TODO',
                    priority: 'URGENT',
                    dueAt: '2026-09-13T12:00:00Z',
                    updatedAt: '2026-09-14T10:00:00Z',
                    attention: 'OVERDUE',
                    targetUrl: '/projects/project-1?task=task-1',
                },
                {
                    taskId: 'task-2',
                    projectId: 'project-2',
                    title: 'Resolve deployment blocker',
                    projectName: 'Operations',
                    status: 'BLOCKED',
                    priority: 'HIGH',
                    dueAt: null,
                    updatedAt: '2026-09-14T11:00:00Z',
                    attention: 'BLOCKED',
                    targetUrl: '/projects/project-2?task=task-2',
                },
            ],
        },
        isPending: false,
        isError: false,
        isFetching: false,
        refetch: vi.fn(),
    }),
}))

describe('MyWorkPage', () => {
    it('renders attention groups and deep links to assigned tasks', () => {
        render(
            <MemoryRouter>
                <MyWorkPage />
            </MemoryRouter>,
        )

        expect(screen.getByRole('heading', { name: 'My Work' })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: 'Overdue' })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: 'Blocked' })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: /Repair billing cancellation/ })).toHaveAttribute(
            'href',
            '/projects/project-1?task=task-1',
        )
    })
})
