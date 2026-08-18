import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { ProjectTask } from '../types/projectTasks'
import { projectTasksApi } from './projectTasksApi'

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

function successfulResponse(data: unknown) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-01T10:30:00Z',
        },
    }
}

describe('projectTasksApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads paginated project tasks with query parameters', async () => {
        const page = {
            content: [task],
            page: 0,
            size: 10,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
        }
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue(successfulResponse(page))
        const params = {
            page: 0,
            size: 10,
            sortBy: 'createdAt' as const,
            sortDir: 'desc' as const,
            status: 'TODO' as const,
            priority: 'HIGH' as const,
            assigneeUserId: 'user-2',
            search: 'access',
        }

        await expect(projectTasksApi.getTasks('tenant-1', 'project-1', params)).resolves.toEqual(
            page,
        )

        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/projects/project-1/tasks', {
            params,
        })
    })

    it('loads one task from the exact tenant and project scoped endpoint', async () => {
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue(successfulResponse(task))

        await expect(projectTasksApi.getTask('tenant-1', 'project-1', 'task-1')).resolves.toEqual(
            task,
        )

        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/projects/project-1/tasks/task-1')
    })

    it('uses the exact task lifecycle endpoints', async () => {
        const post = vi.spyOn(httpClient, 'post').mockResolvedValue(successfulResponse(task))
        const put = vi.spyOn(httpClient, 'put').mockResolvedValue(successfulResponse(task))
        const patch = vi.spyOn(httpClient, 'patch').mockResolvedValue(successfulResponse(task))
        const remove = vi.spyOn(httpClient, 'delete').mockResolvedValue(successfulResponse(task))
        const basePath = '/api/tenants/tenant-1/projects/project-1/tasks'

        await projectTasksApi.createTask('tenant-1', 'project-1', {
            title: 'Review access controls',
            description: null,
            priority: 'HIGH',
            dueAt: null,
            assigneeUserId: 'user-2',
        })
        await projectTasksApi.updateTask('tenant-1', 'project-1', 'task-1', {
            title: 'Review authorization',
            description: null,
            priority: 'URGENT',
            dueAt: null,
        })
        await projectTasksApi.updateTaskStatus('tenant-1', 'project-1', 'task-1', {
            status: 'IN_PROGRESS',
        })
        await projectTasksApi.updateTaskAssignee('tenant-1', 'project-1', 'task-1', {
            assigneeUserId: null,
        })
        await projectTasksApi.cancelTask('tenant-1', 'project-1', 'task-1')

        expect(post).toHaveBeenCalledWith(basePath, {
            title: 'Review access controls',
            description: null,
            priority: 'HIGH',
            dueAt: null,
            assigneeUserId: 'user-2',
        })
        expect(put).toHaveBeenCalledWith(`${basePath}/task-1`, {
            title: 'Review authorization',
            description: null,
            priority: 'URGENT',
            dueAt: null,
        })
        expect(patch).toHaveBeenNthCalledWith(1, `${basePath}/task-1/status`, {
            status: 'IN_PROGRESS',
        })
        expect(patch).toHaveBeenNthCalledWith(2, `${basePath}/task-1/assignee`, {
            assigneeUserId: null,
        })
        expect(remove).toHaveBeenCalledWith(`${basePath}/task-1`)
    })
})
