import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import type { PageResponse } from '../../../types/api'
import { projectMembersApi } from '../api/projectMembersApi'
import { projectTasksApi } from '../api/projectTasksApi'
import type { ProjectTask } from '../types/projectTasks'
import type { ProjectMember } from '../types/projects'
import { ProjectTasksSection } from './ProjectTasksSection'

const member: ProjectMember = {
    membershipId: 'membership-1',
    projectId: 'project-1',
    userId: 'user-2',
    fullName: 'Grace User',
    email: 'grace@example.com',
    tenantRole: 'TENANT_USER',
    userStatus: 'ACTIVE',
    projectRole: 'MEMBER',
    assignedByUserId: 'user-1',
    assignedByUserName: 'Ada Admin',
    assignedByUserEmail: 'ada@example.com',
    assignedAt: '2026-07-16T10:30:00Z',
    updatedAt: '2026-07-16T10:30:00Z',
}

const task: ProjectTask = {
    id: 'task-1',
    tenantId: 'tenant-1',
    projectId: 'project-1',
    title: 'Review access controls',
    description: 'Validate project task authorization.',
    status: 'TODO',
    priority: 'HIGH',
    assigneeUserId: 'user-2',
    assigneeName: 'Grace User',
    assigneeEmail: 'grace@example.com',
    createdByUserId: 'user-1',
    createdByUserName: 'Ada Admin',
    createdByUserEmail: 'ada@example.com',
    dueAt: '2026-08-10T10:30:00Z',
    completedAt: null,
    createdAt: '2026-08-01T10:30:00Z',
    updatedAt: '2026-08-01T10:30:00Z',
}

const membersPage: PageResponse<ProjectMember> = {
    content: [member],
    page: 0,
    size: 100,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

const tasksPage: PageResponse<ProjectTask> = {
    content: [task],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })
}

function renderTasksSection(
    props: Partial<
        React.ComponentProps<typeof ProjectTasksSection>
    > = {},
) {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    {children}
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(
        <ProjectTasksSection
            onFeedback={vi.fn()}
            projectArchived={false}
            projectId="project-1"
            tenantId="tenant-1"
            tenantRole="TENANT_ADMIN"
            userId="user-1"
            {...props}
        />,
        { wrapper: Wrapper },
    )
}

describe('ProjectTasksSection', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(
            projectMembersApi,
            'getMembers',
        ).mockResolvedValue(membersPage)
        vi.spyOn(
            projectMembersApi,
            'getMember',
        ).mockResolvedValue(member)
        vi.spyOn(
            projectTasksApi,
            'getTasks',
        ).mockResolvedValue(tasksPage)
    })

    it('renders task data and management controls for an administrator', async () => {
        renderTasksSection()

        expect(
            await screen.findByText('Review access controls'),
        ).toBeInTheDocument()
        expect(screen.getByText('Grace User')).toBeInTheDocument()
        expect(
            screen.getByRole('button', { name: /create task/i }),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('button', {
                name: /manage review access controls/i,
            }),
        ).toBeInTheDocument()
    })

    it('submits server-side search, status, priority, and assignee filters', async () => {
        const getTasks = vi
            .spyOn(projectTasksApi, 'getTasks')
            .mockResolvedValue(tasksPage)

        renderTasksSection()
        await screen.findByText('Review access controls')

        fireEvent.change(
            screen.getByLabelText(/search project tasks/i),
            { target: { value: '  access  ' } },
        )
        fireEvent.mouseDown(screen.getByLabelText(/^status$/i))
        fireEvent.click(
            screen.getByRole('option', { name: 'To do' }),
        )
        fireEvent.mouseDown(screen.getByLabelText(/^priority$/i))
        fireEvent.click(
            screen.getByRole('option', { name: 'High' }),
        )
        fireEvent.mouseDown(screen.getByLabelText(/^assignee$/i))
        fireEvent.click(
            screen.getByRole('option', { name: 'Grace User' }),
        )

        const searchForm = screen
            .getByLabelText(/search project tasks/i)
            .closest('form')

        fireEvent.click(
            within(searchForm!).getByRole('button', {
                name: /^search$/i,
            }),
        )

        expect(getTasks).toHaveBeenLastCalledWith(
            'tenant-1',
            'project-1',
            expect.objectContaining({
                page: 0,
                status: 'TODO',
                priority: 'HIGH',
                assigneeUserId: 'user-2',
                search: 'access',
            }),
        )
    })

    it('creates a normalized task with an optional assignee', async () => {
        const user = userEvent.setup()
        const createTask = vi
            .spyOn(projectTasksApi, 'createTask')
            .mockResolvedValue(task)

        renderTasksSection()
        await screen.findByText('Review access controls')
        await user.click(
            screen.getByRole('button', { name: /create task/i }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /create task/i,
        })
        fireEvent.change(
            within(dialog).getByLabelText(/task title/i),
            {
                target: {
                    value: '  Audit refresh flow  ',
                },
            },
        )
        fireEvent.change(
            within(dialog).getByLabelText(/description/i),
            {
                target: {
                    value: '  Validate token rotation.  ',
                },
            },
        )
        fireEvent.mouseDown(
            within(dialog).getByLabelText(/^assignee$/i),
        )
        fireEvent.click(
            screen.getByRole('option', {
                name: /grace user.*grace@example.com/i,
            }),
        )
        fireEvent.click(
            within(dialog).getByRole('button', {
                name: /^create task$/i,
            }),
        )

        await waitFor(() => {
            expect(createTask).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                {
                    title: 'Audit refresh flow',
                    description: 'Validate token rotation.',
                    priority: 'MEDIUM',
                    dueAt: null,
                    assigneeUserId: 'user-2',
                },
            )
        })
    })

    it('lets an assigned member change only task status', async () => {
        const user = userEvent.setup()
        const updateStatus = vi
            .spyOn(projectTasksApi, 'updateTaskStatus')
            .mockResolvedValue({
                ...task,
                status: 'IN_PROGRESS',
            })

        renderTasksSection({
            tenantRole: 'TENANT_USER',
            userId: 'user-2',
        })
        await screen.findByText('Review access controls')

        expect(
            screen.queryByRole('button', { name: /create task/i }),
        ).not.toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /manage review access controls/i,
            }),
        )
        expect(
            screen.getByRole('menuitem', { name: /change status/i }),
        ).toBeInTheDocument()
        expect(
            screen.queryByRole('menuitem', { name: /edit task/i }),
        ).not.toBeInTheDocument()
        await user.click(
            screen.getByRole('menuitem', { name: /change status/i }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /change task status/i,
        })
        await user.click(within(dialog).getByLabelText(/^status$/i))
        await user.click(
            screen.getByRole('option', { name: 'In progress' }),
        )
        await user.click(
            within(dialog).getByRole('button', {
                name: /change status/i,
            }),
        )

        await waitFor(() => {
            expect(updateStatus).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                'task-1',
                { status: 'IN_PROGRESS' },
            )
        })
    })

    it('gives a project lead complete task controls', async () => {
        vi.spyOn(
            projectMembersApi,
            'getMember',
        ).mockResolvedValue({
            ...member,
            projectRole: 'PROJECT_LEAD',
        })

        renderTasksSection({
            tenantRole: 'TENANT_USER',
            userId: 'user-2',
        })

        expect(
            await screen.findByRole('button', {
                name: /create task/i,
            }),
        ).toBeInTheDocument()
    })

    it('keeps archived projects and cancelled tasks immutable', async () => {
        vi.spyOn(projectTasksApi, 'getTasks').mockResolvedValue({
            ...tasksPage,
            content: [{ ...task, status: 'CANCELLED' }],
        })

        renderTasksSection({ projectArchived: true })
        await screen.findByText('Review access controls')

        expect(
            screen.getByRole('button', { name: /create task/i }),
        ).toBeDisabled()
        expect(
            screen.queryByRole('button', {
                name: /manage review access controls/i,
            }),
        ).not.toBeInTheDocument()
    })
})
