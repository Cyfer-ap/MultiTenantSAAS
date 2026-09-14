import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { TaskPlanningPage } from './TaskPlanningPage'

const mocks = vi.hoisted(() => ({
    useGlobalSearch: vi.fn(),
    useProjectTask: vi.fn(),
    useProjectDetails: vi.fn(),
    useProjectMember: vi.fn(),
    useCurrentAuthorization: vi.fn(),
    hasProjectPermission: vi.fn(),
}))

vi.mock('../../auth/hooks/useAuth', () => ({
    useAuth: () => ({ session: { tenantId: 'tenant-1', userId: 'user-1' } }),
}))

vi.mock('../../search/hooks/useGlobalSearch', () => ({
    useGlobalSearch: mocks.useGlobalSearch,
}))

vi.mock('../../projects/hooks/useProjectTasks', () => ({
    useProjectTask: mocks.useProjectTask,
}))

vi.mock('../../projects/hooks/useProjectDetails', () => ({
    useProjectDetails: mocks.useProjectDetails,
}))

vi.mock('../../projects/hooks/useProjectMembers', () => ({
    useProjectMember: mocks.useProjectMember,
}))

vi.mock('../../authorization/hooks/useCurrentAuthorization', () => ({
    useCurrentAuthorization: mocks.useCurrentAuthorization,
}))

vi.mock('../../authorization/access/authorizationAccess', () => ({
    hasProjectPermission: mocks.hasProjectPermission,
}))

vi.mock('../components/TaskRelationshipsPanel', () => ({
    TaskRelationshipsPanel: ({ task }: { task: { title: string } }) => (
        <div data-testid="task-relationships-panel">Planning {task.title}</div>
    ),
}))

const task = {
    id: 'task-1',
    tenantId: 'tenant-1',
    projectId: 'project-1',
    title: 'Ship release',
    description: 'Coordinate the release.',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    assigneeUserId: null,
    assigneeName: null,
    assigneeEmail: null,
    createdByUserId: 'user-1',
    createdByUserName: 'Ada Admin',
    createdByUserEmail: 'ada@example.com',
    dueAt: null,
    completedAt: null,
    createdAt: '2026-09-14T00:00:00Z',
    updatedAt: '2026-09-14T00:00:00Z',
}

describe('TaskPlanningPage', () => {
    beforeEach(() => {
        Object.values(mocks).forEach((mockFn) => mockFn.mockReset())
        mocks.useCurrentAuthorization.mockReturnValue({ data: {} })
        mocks.hasProjectPermission.mockReturnValue(true)
        mocks.useProjectMember.mockReturnValue({ data: undefined })
        mocks.useProjectDetails.mockImplementation((_tenantId: string, projectId: string) => ({
            data: projectId ? { id: projectId, name: 'Phoenix', status: 'ACTIVE' } : undefined,
        }))
        mocks.useProjectTask.mockImplementation(
            (_tenantId: string, _projectId: string, taskId: string) => ({
                data: taskId ? task : undefined,
                isPending: false,
                isError: false,
                error: null,
            }),
        )
        mocks.useGlobalSearch.mockReturnValue({
            data: { query: '', results: [] },
            isPending: false,
            isError: false,
            isSuccess: true,
            error: null,
        })
    })

    it('renders the planning workspace with no task selected', () => {
        render(
            <MemoryRouter initialEntries={['/task-planning']}>
                <TaskPlanningPage />
            </MemoryRouter>,
        )

        expect(screen.getByRole('heading', { name: 'Task Planning' })).toBeInTheDocument()
        expect(screen.getByText(/select a task to plan/i)).toBeInTheDocument()
    })

    it('selects an authorization-safe task search result and opens its planning panel', async () => {
        const user = userEvent.setup()
        mocks.useGlobalSearch.mockImplementation((_tenantId: string, query: string) => ({
            data: {
                query,
                results:
                    query.length >= 2
                        ? [
                              {
                                  type: 'TASK',
                                  id: 'task-1',
                                  parentId: 'project-1',
                                  title: 'Ship release',
                                  subtitle: 'Phoenix',
                              },
                          ]
                        : [],
            },
            isPending: false,
            isError: false,
            isSuccess: true,
            error: null,
        }))

        render(
            <MemoryRouter initialEntries={['/task-planning']}>
                <TaskPlanningPage />
            </MemoryRouter>,
        )

        await user.type(screen.getByLabelText(/search accessible tasks/i), 'ship')
        await user.click(screen.getByRole('button', { name: /ship release/i }))

        expect(await screen.findByTestId('task-relationships-panel')).toHaveTextContent(
            'Ship release',
        )
        expect(screen.getByRole('link', { name: /open project task/i })).toHaveAttribute(
            'href',
            '/projects/project-1?task=task-1',
        )
    })
})
