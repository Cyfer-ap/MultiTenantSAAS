import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { ProjectMember } from '../types/projects'
import { projectMembersApi } from './projectMembersApi'

const projectMember: ProjectMember = {
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

describe('projectMembersApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads paginated project members with query parameters', async () => {
        const page = {
            content: [projectMember],
            page: 0,
            size: 10,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
        }
        const get = vi
            .spyOn(httpClient, 'get')
            .mockResolvedValue(successfulResponse(page))
        const params = {
            page: 0,
            size: 10,
            sortBy: 'assignedAt' as const,
            sortDir: 'asc' as const,
            role: 'MEMBER' as const,
            search: 'grace',
        }

        await expect(
            projectMembersApi.getMembers(
                'tenant-1',
                'project-1',
                params,
            ),
        ).resolves.toEqual(page)

        expect(get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/projects/project-1/members',
            { params },
        )
    })

    it('uses the dedicated add, role, and remove endpoints', async () => {
        const post = vi
            .spyOn(httpClient, 'post')
            .mockResolvedValue(
                successfulResponse(projectMember),
            )
        const patch = vi
            .spyOn(httpClient, 'patch')
            .mockResolvedValue(
                successfulResponse(projectMember),
            )
        const remove = vi
            .spyOn(httpClient, 'delete')
            .mockResolvedValue(
                successfulResponse(projectMember),
            )

        await projectMembersApi.addMember(
            'tenant-1',
            'project-1',
            { userId: 'user-2', role: 'MEMBER' },
        )
        await projectMembersApi.updateMemberRole(
            'tenant-1',
            'project-1',
            'user-2',
            { role: 'PROJECT_LEAD' },
        )
        await projectMembersApi.removeMember(
            'tenant-1',
            'project-1',
            'user-2',
        )

        const basePath =
            '/api/tenants/tenant-1/projects/project-1/members'

        expect(post).toHaveBeenCalledWith(basePath, {
            userId: 'user-2',
            role: 'MEMBER',
        })
        expect(patch).toHaveBeenCalledWith(
            `${basePath}/user-2/role`,
            { role: 'PROJECT_LEAD' },
        )
        expect(remove).toHaveBeenCalledWith(
            `${basePath}/user-2`,
        )
    })
})
