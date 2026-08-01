import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { TenantProject } from '../types/projects'
import { projectsApi } from './projectsApi'

const tenantProject: TenantProject = {
    id: 'project-1',
    tenantId: 'tenant-1',
    name: 'Research workspace',
    description: 'Coordinate the research programme.',
    status: 'PLANNING',
    createdByUserId: 'user-1',
    createdByUserName: 'Ada Admin',
    createdByUserEmail: 'ada@example.com',
    createdAt: '2026-07-16T10:30:00Z',
    updatedAt: '2026-07-16T10:30:00Z',
}

function successfulResponse(project: TenantProject) {
    return {
        data: {
            success: true,
            message: 'Success',
            data: project,
            timestamp: '2026-08-01T10:30:00Z',
        },
    }
}

describe('projectsApi management operations', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('creates and updates a project in the selected tenant', async () => {
        const post = vi
            .spyOn(httpClient, 'post')
            .mockResolvedValue(successfulResponse(tenantProject))
        const put = vi
            .spyOn(httpClient, 'put')
            .mockResolvedValue(successfulResponse(tenantProject))
        const input = {
            name: 'Research workspace',
            description: 'Coordinate the research programme.',
        }

        await projectsApi.createProject('tenant-1', input)
        await projectsApi.updateProject(
            'tenant-1',
            'project-1',
            input,
        )

        expect(post).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/projects',
            input,
        )
        expect(put).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/projects/project-1',
            input,
        )
    })

    it('uses the dedicated status and archive endpoints', async () => {
        const patch = vi
            .spyOn(httpClient, 'patch')
            .mockResolvedValue(successfulResponse(tenantProject))
        const remove = vi
            .spyOn(httpClient, 'delete')
            .mockResolvedValue(successfulResponse({
                ...tenantProject,
                status: 'ARCHIVED',
            }))

        await projectsApi.updateProjectStatus(
            'tenant-1',
            'project-1',
            { status: 'ACTIVE' },
        )
        await projectsApi.archiveProject(
            'tenant-1',
            'project-1',
        )

        expect(patch).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/projects/project-1/status',
            { status: 'ACTIVE' },
        )
        expect(remove).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/projects/project-1',
        )
    })
})
