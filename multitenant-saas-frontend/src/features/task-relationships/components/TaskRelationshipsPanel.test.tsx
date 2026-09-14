import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { ProjectTask } from '../../projects/types/projectTasks'
import { TaskRelationshipsPanel } from './TaskRelationshipsPanel'

const mocks = vi.hoisted(() => ({
    useTaskRelationships: vi.fn(),
    useProjectTaskLabels: vi.fn(),
    useUpdateTaskParent: vi.fn(),
    useAddTaskDependency: vi.fn(),
    useRemoveTaskDependency: vi.fn(),
    useAssignTaskLabel: vi.fn(),
    useUnassignTaskLabel: vi.fn(),
    useCreateTaskLabel: vi.fn(),
    useUpdateTaskLabel: vi.fn(),
    useDeleteTaskLabel: vi.fn(),
    useProjectTasks: vi.fn(),
}))

vi.mock('../hooks/useTaskRelationships', () => ({
    useTaskRelationships: mocks.useTaskRelationships,
    useProjectTaskLabels: mocks.useProjectTaskLabels,
    useUpdateTaskParent: mocks.useUpdateTaskParent,
    useAddTaskDependency: mocks.useAddTaskDependency,
    useRemoveTaskDependency: mocks.useRemoveTaskDependency,
    useAssignTaskLabel: mocks.useAssignTaskLabel,
    useUnassignTaskLabel: mocks.useUnassignTaskLabel,
    useCreateTaskLabel: mocks.useCreateTaskLabel,
    useUpdateTaskLabel: mocks.useUpdateTaskLabel,
    useDeleteTaskLabel: mocks.useDeleteTaskLabel,
}))

vi.mock('../../projects/hooks/useProjectTasks', () => ({
    useProjectTasks: mocks.useProjectTasks,
}))

const task: ProjectTask = {
    id: 'task-1',
    tenantId: 'tenant-1',
    projectId: 'project-1',
    title: 'Ship release',
    description: null,
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

function mutation() {
    return { isPending: false, mutateAsync: vi.fn() }
}

describe('TaskRelationshipsPanel', () => {
    beforeEach(() => {
        Object.values(mocks).forEach((mockFn) => mockFn.mockReset())
        mocks.useTaskRelationships.mockReturnValue({
            data: {
                parent: { id: 'task-parent', title: 'Release epic', status: 'TODO', priority: 'HIGH' },
                children: [
                    { id: 'task-child', title: 'Publish notes', status: 'TODO', priority: 'MEDIUM' },
                ],
                blockers: [
                    { id: 'task-blocker', title: 'Security review', status: 'BLOCKED', priority: 'URGENT' },
                ],
                dependents: [
                    { id: 'task-dependent', title: 'Announce release', status: 'TODO', priority: 'LOW' },
                ],
                labels: [{ id: 'label-1', name: 'Release', color: '#4F46E5' }],
                childrenTruncated: false,
                blockersTruncated: false,
                dependentsTruncated: false,
            },
            isPending: false,
            isError: false,
            error: null,
        })
        mocks.useProjectTaskLabels.mockReturnValue({
            data: [
                { id: 'label-1', name: 'Release', color: '#4F46E5' },
                { id: 'label-2', name: 'Customer', color: null },
            ],
            isPending: false,
            isError: false,
        })
        mocks.useProjectTasks.mockReturnValue({
            data: { content: [], totalElements: 0 },
            isPending: false,
            isError: false,
            isSuccess: true,
        })
        mocks.useUpdateTaskParent.mockReturnValue(mutation())
        mocks.useAddTaskDependency.mockReturnValue(mutation())
        mocks.useRemoveTaskDependency.mockReturnValue(mutation())
        mocks.useAssignTaskLabel.mockReturnValue(mutation())
        mocks.useUnassignTaskLabel.mockReturnValue(mutation())
        mocks.useCreateTaskLabel.mockReturnValue(mutation())
        mocks.useUpdateTaskLabel.mockReturnValue(mutation())
        mocks.useDeleteTaskLabel.mockReturnValue(mutation())
    })

    it('renders hierarchy, dependency and label context and opens related tasks', async () => {
        const user = userEvent.setup()
        const onOpenTask = vi.fn()

        render(
            <TaskRelationshipsPanel
                canManage
                onFeedback={vi.fn()}
                onOpenTask={onOpenTask}
                projectId="project-1"
                task={task}
                tenantId="tenant-1"
            />,
        )

        expect(screen.getByText('Release epic')).toBeInTheDocument()
        expect(screen.getByText('Publish notes')).toBeInTheDocument()
        expect(screen.getByText('Security review')).toBeInTheDocument()
        expect(screen.getByText('Announce release')).toBeInTheDocument()
        expect(screen.getByText('Release')).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: /release epic/i }))
        expect(onOpenTask).toHaveBeenCalledWith('task-parent')
    })

    it('hides relationship mutation controls for read-only access', () => {
        render(
            <TaskRelationshipsPanel
                canManage={false}
                onFeedback={vi.fn()}
                onOpenTask={vi.fn()}
                projectId="project-1"
                task={task}
                tenantId="tenant-1"
            />,
        )

        expect(screen.queryByRole('button', { name: /change parent/i })).not.toBeInTheDocument()
        expect(screen.queryByRole('button', { name: /add blocker/i })).not.toBeInTheDocument()
        expect(screen.queryByRole('button', { name: /manage project labels/i })).not.toBeInTheDocument()
    })
})
