import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { TaskActivity, TaskComment } from '../types/taskCollaboration'
import { projectTaskCollaborationApi } from './projectTaskCollaborationApi'

const comment: TaskComment = {
    id: 'comment-1',
    taskId: 'task-1',
    parentCommentId: null,
    authorUserId: 'user-1',
    authorName: 'Ada Admin',
    authorEmail: 'ada@example.com',
    body: 'Please review this with Grace.',
    deleted: false,
    replyCount: 1,
    pinned: true,
    pinnedAt: '2026-08-18T01:05:00Z',
    pinnedByUserId: 'user-1',
    editedAt: null,
    deletedAt: null,
    createdAt: '2026-08-18T01:00:00Z',
    updatedAt: '2026-08-18T01:05:00Z',
    mentions: [{ userId: 'user-2', fullName: 'Grace User', email: 'grace@example.com' }],
}

const reply: TaskComment = {
    ...comment,
    id: 'reply-1',
    parentCommentId: 'comment-1',
    body: 'Reviewed — looks good.',
    replyCount: 0,
    pinned: false,
    pinnedAt: null,
    pinnedByUserId: null,
}

const activity: TaskActivity = {
    id: 'activity-1',
    taskId: 'task-1',
    type: 'COMMENT_REPLIED',
    actorUserId: 'user-1',
    actorName: 'Ada Admin',
    actorEmail: 'ada@example.com',
    summary: 'Replied to a comment',
    createdAt: '2026-08-18T01:00:00Z',
}

function successfulResponse(data: unknown) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-18T01:00:00Z',
        },
    }
}

describe('projectTaskCollaborationApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('uses tenant, project, task, and comment scoped collaboration endpoints', async () => {
        const page = {
            content: [comment],
            page: 0,
            size: 50,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
        }
        const replyPage = { ...page, content: [reply] }
        const activityPage = { ...page, content: [activity] }
        const get = vi
            .spyOn(httpClient, 'get')
            .mockResolvedValueOnce(successfulResponse(page))
            .mockResolvedValueOnce(successfulResponse([comment]))
            .mockResolvedValueOnce(successfulResponse(replyPage))
            .mockResolvedValueOnce(successfulResponse(activityPage))
        const post = vi.spyOn(httpClient, 'post').mockResolvedValue(successfulResponse(comment))
        const put = vi.spyOn(httpClient, 'put').mockResolvedValue(successfulResponse(comment))
        const remove = vi.spyOn(httpClient, 'delete').mockResolvedValue(successfulResponse(comment))
        const descParams = { page: 0, size: 50, sortDir: 'desc' as const }
        const ascParams = { page: 0, size: 50, sortDir: 'asc' as const }
        const input = { body: 'Please review this with Grace.', mentionedUserIds: ['user-2'] }
        const base = '/api/tenants/tenant-1/projects/project-1/tasks/task-1'

        await projectTaskCollaborationApi.getComments('tenant-1', 'project-1', 'task-1', descParams)
        await projectTaskCollaborationApi.getPinnedComments('tenant-1', 'project-1', 'task-1')
        await projectTaskCollaborationApi.getReplies(
            'tenant-1',
            'project-1',
            'task-1',
            'comment-1',
            ascParams,
        )
        await projectTaskCollaborationApi.createComment('tenant-1', 'project-1', 'task-1', input)
        await projectTaskCollaborationApi.createReply(
            'tenant-1',
            'project-1',
            'task-1',
            'comment-1',
            input,
        )
        await projectTaskCollaborationApi.updateComment(
            'tenant-1',
            'project-1',
            'task-1',
            'comment-1',
            input,
        )
        await projectTaskCollaborationApi.deleteComment(
            'tenant-1',
            'project-1',
            'task-1',
            'comment-1',
        )
        await projectTaskCollaborationApi.pinComment('tenant-1', 'project-1', 'task-1', 'comment-1')
        await projectTaskCollaborationApi.unpinComment(
            'tenant-1',
            'project-1',
            'task-1',
            'comment-1',
        )
        await projectTaskCollaborationApi.getActivity('tenant-1', 'project-1', 'task-1', descParams)

        expect(get).toHaveBeenNthCalledWith(1, `${base}/comments`, { params: descParams })
        expect(get).toHaveBeenNthCalledWith(2, `${base}/comments/pinned`)
        expect(get).toHaveBeenNthCalledWith(3, `${base}/comments/comment-1/replies`, {
            params: ascParams,
        })
        expect(post).toHaveBeenCalledWith(`${base}/comments`, input)
        expect(post).toHaveBeenCalledWith(`${base}/comments/comment-1/replies`, input)
        expect(post).toHaveBeenCalledWith(`${base}/comments/comment-1/pin`)
        expect(put).toHaveBeenCalledWith(`${base}/comments/comment-1`, input)
        expect(remove).toHaveBeenCalledWith(`${base}/comments/comment-1`)
        expect(remove).toHaveBeenCalledWith(`${base}/comments/comment-1/pin`)
        expect(get).toHaveBeenNthCalledWith(4, `${base}/activity`, { params: descParams })
    })
})
