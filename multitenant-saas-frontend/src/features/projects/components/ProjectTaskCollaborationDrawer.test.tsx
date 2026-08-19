import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import type { PageResponse } from '../../../types/api'
import { projectTaskCollaborationApi } from '../api/projectTaskCollaborationApi'
import type { TaskActivity, TaskComment } from '../types/taskCollaboration'
import type { ProjectTask } from '../types/projectTasks'
import type { ProjectMember } from '../types/projects'
import { ProjectTaskCollaborationDrawer } from './ProjectTaskCollaborationDrawer'

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
    dueAt: '2026-08-20T10:30:00Z',
    completedAt: null,
    createdAt: '2026-08-01T10:30:00Z',
    updatedAt: '2026-08-01T10:30:00Z',
}

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
    assignedAt: '2026-08-01T10:30:00Z',
    updatedAt: '2026-08-01T10:30:00Z',
}

const comment: TaskComment = {
    id: 'comment-1',
    taskId: 'task-1',
    parentCommentId: null,
    authorUserId: 'user-1',
    authorName: 'Ada Admin',
    authorEmail: 'ada@example.com',
    body: 'Please validate this before release.',
    deleted: false,
    replyCount: 1,
    pinned: false,
    pinnedAt: null,
    pinnedByUserId: null,
    editedAt: null,
    deletedAt: null,
    createdAt: '2026-08-18T01:00:00Z',
    updatedAt: '2026-08-18T01:00:00Z',
    mentions: [],
}

const reply: TaskComment = {
    ...comment,
    id: 'reply-1',
    parentCommentId: 'comment-1',
    authorUserId: 'user-2',
    authorName: 'Grace User',
    authorEmail: 'grace@example.com',
    body: 'I checked it and the scope looks correct.',
    replyCount: 0,
    pinned: false,
}

const activity: TaskActivity = {
    id: 'activity-1',
    taskId: 'task-1',
    type: 'TASK_CREATED',
    actorUserId: 'user-1',
    actorName: 'Ada Admin',
    actorEmail: 'ada@example.com',
    summary: 'Task created',
    createdAt: '2026-08-01T10:30:00Z',
}

function page<T>(content: T[]): PageResponse<T> {
    return {
        content,
        page: 0,
        size: 50,
        totalElements: content.length,
        totalPages: content.length === 0 ? 0 : 1,
        first: true,
        last: true,
    }
}

function renderDrawer(readOnly = false) {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(
        <ProjectTaskCollaborationDrawer
            currentUserId="user-1"
            members={[member]}
            onClose={vi.fn()}
            onFeedback={vi.fn()}
            open
            projectId="project-1"
            readOnly={readOnly}
            task={task}
            tenantId="tenant-1"
        />,
        { wrapper: Wrapper },
    )
}

describe('ProjectTaskCollaborationDrawer', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        window.history.replaceState({}, '', '/projects/project-1?task=task-1')
        Element.prototype.scrollIntoView = vi.fn()
        vi.spyOn(projectTaskCollaborationApi, 'getComments').mockResolvedValue(page([comment]))
        vi.spyOn(projectTaskCollaborationApi, 'getComment').mockImplementation(
            async (_tenantId, _projectId, _taskId, commentId) =>
                commentId === reply.id ? reply : comment,
        )
        vi.spyOn(projectTaskCollaborationApi, 'getPinnedComments').mockResolvedValue([])
        vi.spyOn(projectTaskCollaborationApi, 'getReplies').mockResolvedValue(page([reply]))
        vi.spyOn(projectTaskCollaborationApi, 'getActivity').mockResolvedValue(page([activity]))
    })

    it('creates a comment with a selected project-member mention', async () => {
        const user = userEvent.setup()
        const createComment = vi
            .spyOn(projectTaskCollaborationApi, 'createComment')
            .mockResolvedValue({
                ...comment,
                replyCount: 0,
                body: 'Grace, can you review this?',
                mentions: [
                    {
                        userId: member.userId,
                        fullName: member.fullName,
                        email: member.email,
                    },
                ],
            })

        renderDrawer()
        expect(await screen.findByText('Please validate this before release.')).toBeInTheDocument()

        await user.type(screen.getByLabelText(/add a comment/i), 'Grace, can you review this?')
        const mentionInput = screen.getByLabelText(/mention teammates/i)
        await user.click(mentionInput)
        await user.click(await screen.findByRole('option', { name: /grace user/i }))
        await user.click(screen.getByRole('button', { name: /^comment$/i }))

        await waitFor(() => {
            expect(createComment).toHaveBeenCalledWith('tenant-1', 'project-1', 'task-1', {
                body: 'Grace, can you review this?',
                mentionedUserIds: ['user-2'],
            })
        })
    })

    it('loads a one-level thread and creates a reply', async () => {
        const user = userEvent.setup()
        const createReply = vi
            .spyOn(projectTaskCollaborationApi, 'createReply')
            .mockResolvedValue(reply)

        renderDrawer()
        await screen.findByText('Please validate this before release.')

        await user.click(screen.getByRole('button', { name: /view 1 reply/i }))
        expect(
            await screen.findByText('I checked it and the scope looks correct.'),
        ).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: /^reply$/i }))
        await user.type(screen.getByLabelText(/write a reply/i), 'Thanks, that resolves it.')
        await user.click(screen.getByRole('button', { name: /post reply/i }))

        await waitFor(() => {
            expect(createReply).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                'task-1',
                'comment-1',
                { body: 'Thanks, that resolves it.', mentionedUserIds: [] },
            )
        })
    })

    it('opens and highlights a top-level comment deep link', async () => {
        window.history.replaceState(
            {},
            '',
            '/projects/project-1?task=task-1&comment=comment-1',
        )

        renderDrawer()

        expect(await screen.findByText('Please validate this before release.')).toBeInTheDocument()
        await waitFor(() => {
            expect(
                document.querySelector('[data-comment-id="comment-1"]'),
            ).toHaveAttribute('data-deep-link-target', 'true')
        })
        expect(projectTaskCollaborationApi.getComment).toHaveBeenCalledWith(
            'tenant-1',
            'project-1',
            'task-1',
            'comment-1',
        )
    })

    it('expands a linked thread and highlights the exact reply', async () => {
        window.history.replaceState(
            {},
            '',
            '/projects/project-1?task=task-1&comment=comment-1&reply=reply-1',
        )

        renderDrawer()

        expect(
            await screen.findByText('I checked it and the scope looks correct.'),
        ).toBeInTheDocument()
        await waitFor(() => {
            expect(document.querySelector('[data-comment-id="reply-1"]')).toHaveAttribute(
                'data-deep-link-target',
                'true',
            )
        })
    })

    it('renders a referenced comment even when it is outside the first comment page', async () => {
        const historicalComment: TaskComment = {
            ...comment,
            id: 'comment-old',
            body: 'Historical context from an older page.',
            replyCount: 0,
        }
        vi.spyOn(projectTaskCollaborationApi, 'getComments').mockResolvedValue(page([]))
        vi.spyOn(projectTaskCollaborationApi, 'getComment').mockResolvedValue(historicalComment)
        window.history.replaceState(
            {},
            '',
            '/projects/project-1?task=task-1&comment=comment-old',
        )

        renderDrawer()

        expect(await screen.findByText('Referenced comment')).toBeInTheDocument()
        expect(await screen.findByText('Historical context from an older page.')).toBeInTheDocument()
        expect(document.querySelector('[data-comment-id="comment-old"]')).toHaveAttribute(
            'data-deep-link-target',
            'true',
        )
    })

    it('cleans comment and reply parameters when the drawer closes', async () => {
        const user = userEvent.setup()
        window.history.replaceState(
            {},
            '',
            '/projects/project-1?task=task-1&comment=comment-1&reply=reply-1',
        )
        renderDrawer()

        await screen.findByText('I checked it and the scope looks correct.')
        await user.click(screen.getByRole('button', { name: /close task details/i }))

        expect(window.location.search).toBe('?task=task-1')
    })

    it('shows pinned comments separately and can unpin them', async () => {
        const user = userEvent.setup()
        const pinnedComment: TaskComment = {
            ...comment,
            pinned: true,
            pinnedAt: '2026-08-18T02:00:00Z',
            pinnedByUserId: 'user-1',
        }
        vi.spyOn(projectTaskCollaborationApi, 'getPinnedComments').mockResolvedValue([
            pinnedComment,
        ])
        const unpin = vi.spyOn(projectTaskCollaborationApi, 'unpinComment').mockResolvedValue({
            ...pinnedComment,
            pinned: false,
            pinnedAt: null,
            pinnedByUserId: null,
        })

        renderDrawer()

        expect(await screen.findByText('Pinned comments')).toBeInTheDocument()
        expect(screen.getAllByText('Please validate this before release.')).toHaveLength(1)
        await user.click(screen.getByRole('button', { name: /unpin comment by ada admin/i }))

        await waitFor(() => {
            expect(unpin).toHaveBeenCalledWith('tenant-1', 'project-1', 'task-1', 'comment-1')
        })
    })

    it('shows domain activity and keeps archived or cancelled collaboration read only', async () => {
        const user = userEvent.setup()
        renderDrawer(true)

        expect(
            await screen.findByText(/existing discussion remains available for history/i),
        ).toBeInTheDocument()
        expect(screen.queryByLabelText(/add a comment/i)).not.toBeInTheDocument()

        await user.click(screen.getByRole('tab', { name: /activity/i }))

        expect(await screen.findByRole('heading', { name: 'Task created' })).toBeInTheDocument()
        expect(screen.getByText(/ada admin/i)).toBeInTheDocument()
    })

    it('lets the comment author edit and soft-delete their own comment', async () => {
        const user = userEvent.setup()
        const updateComment = vi
            .spyOn(projectTaskCollaborationApi, 'updateComment')
            .mockResolvedValue({ ...comment, body: 'Updated release context.' })
        const deleteComment = vi
            .spyOn(projectTaskCollaborationApi, 'deleteComment')
            .mockResolvedValue({ ...comment, body: null, deleted: true })

        renderDrawer()
        await screen.findByText('Please validate this before release.')

        await user.click(screen.getByRole('button', { name: /edit comment by ada admin/i }))
        const editField = screen.getByRole('textbox', { name: /^edit comment$/i })
        await user.clear(editField)
        await user.type(editField, 'Updated release context.')
        await user.click(screen.getByRole('button', { name: /^save$/i }))

        await waitFor(() => {
            expect(updateComment).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                'task-1',
                'comment-1',
                { body: 'Updated release context.', mentionedUserIds: [] },
            )
        })

        await user.click(screen.getByRole('button', { name: /delete comment by ada admin/i }))
        await user.click(screen.getByRole('button', { name: /^delete$/i }))

        await waitFor(() => {
            expect(deleteComment).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                'task-1',
                'comment-1',
            )
        })
    })
})
