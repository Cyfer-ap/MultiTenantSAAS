import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import type { PageResponse } from '../../../types/api'
import { projectTaskCollaborationApi } from '../api/projectTaskCollaborationApi'
import type { TaskAttachment, TaskComment } from '../types/taskCollaboration'
import { ProjectTaskAttachmentsPanel } from './ProjectTaskAttachmentsPanel'

const comment: TaskComment = {
    id: 'comment-1',
    taskId: 'task-1',
    authorUserId: 'user-1',
    authorName: 'Ada Admin',
    authorEmail: 'ada@example.com',
    body: 'Release evidence',
    deleted: false,
    editedAt: null,
    deletedAt: null,
    createdAt: '2026-08-18T01:00:00Z',
    updatedAt: '2026-08-18T01:00:00Z',
    mentions: [],
}

const attachment: TaskAttachment = {
    id: 'attachment-1',
    taskId: 'task-1',
    commentId: null,
    uploaderUserId: 'user-1',
    uploaderName: 'Ada Admin',
    filename: 'evidence.pdf',
    contentType: 'application/pdf',
    sizeBytes: 1200,
    status: 'AVAILABLE',
    createdAt: '2026-08-18T01:00:00Z',
    completedAt: '2026-08-18T01:00:01Z',
    deletedAt: null,
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

function renderPanel(readOnly = false) {
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
        <ProjectTaskAttachmentsPanel
            comments={[comment]}
            currentUserId="user-1"
            onFeedback={vi.fn()}
            projectId="project-1"
            readOnly={readOnly}
            taskId="task-1"
            tenantId="tenant-1"
        />,
        { wrapper: Wrapper },
    )
}

describe('ProjectTaskAttachmentsPanel', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(projectTaskCollaborationApi, 'getAttachments').mockResolvedValue(
            page([attachment]),
        )
    })

    it('uploads a task attachment through the collaboration API', async () => {
        const user = userEvent.setup()
        const upload = vi
            .spyOn(projectTaskCollaborationApi, 'uploadTaskAttachment')
            .mockResolvedValue(attachment)

        renderPanel()
        expect(await screen.findByText('evidence.pdf')).toBeInTheDocument()

        const file = new File(['hello'], 'notes.txt', { type: 'text/plain' })
        await user.upload(screen.getByLabelText(/choose attachment file/i), file)

        await waitFor(() => {
            expect(upload).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                'task-1',
                expect.objectContaining({ file, commentId: null }),
            )
        })
    })

    it('keeps attachment mutation controls hidden in read-only mode', async () => {
        renderPanel(true)

        expect(
            await screen.findByText(/attachments are read-only for this task/i),
        ).toBeInTheDocument()
        expect(await screen.findByText('evidence.pdf')).toBeInTheDocument()

        expect(screen.queryByLabelText(/choose attachment file/i)).not.toBeInTheDocument()
        expect(
            screen.queryByRole('button', { name: /delete evidence.pdf/i }),
        ).not.toBeInTheDocument()
        expect(screen.getByRole('button', { name: /download evidence.pdf/i })).toBeInTheDocument()
    })
})
