import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import {
    httpClient,
    publicHttpClient,
} from '../../../api/httpClient'
import type {
    CreatedInvitation,
    TenantInvitation,
} from '../types/invitations'
import { invitationsApi } from './invitationsApi'

const invitation: TenantInvitation = {
    invitationId: 'invitation-1',
    tenantId: 'tenant-1',
    fullName: 'Grace User',
    email: 'grace@example.com',
    role: 'TENANT_USER',
    status: 'PENDING',
    active: true,
    expired: false,
    expiresAt: '2026-08-03T10:30:00Z',
    createdAt: '2026-08-01T10:30:00Z',
    acceptedAt: null,
    revokedAt: null,
    invitedByUserId: 'user-1',
    invitedByUserEmail: 'admin@example.com',
    invitedBySystemAdminId: null,
    invitedBySystemAdminEmail: null,
}

const createdInvitation: CreatedInvitation = {
    invitationId: invitation.invitationId,
    tenantId: invitation.tenantId,
    fullName: invitation.fullName,
    email: invitation.email,
    role: invitation.role,
    status: invitation.status,
    expiresAt: invitation.expiresAt,
    devInvitationToken: 'one-time-token',
    message: 'Invitation generated successfully.',
}

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-01T10:31:00Z',
        },
    }
}

describe('invitationsApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads a filtered tenant invitation page', async () => {
        const params = {
            page: 1,
            size: 25,
            sortBy: 'email' as const,
            sortDir: 'asc' as const,
            status: 'PENDING' as const,
            role: 'TENANT_USER' as const,
            search: 'grace',
        }
        const page = {
            content: [invitation],
            page: 1,
            size: 25,
            totalElements: 26,
            totalPages: 2,
            first: false,
            last: true,
        }
        const get = vi
            .spyOn(httpClient, 'get')
            .mockResolvedValue(successfulResponse(page))

        await expect(
            invitationsApi.getInvitations(
                'tenant-1',
                params,
            ),
        ).resolves.toEqual(page)

        expect(get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/user-invitations',
            { params },
        )
    })

    it('creates and revokes invitations through tenant endpoints', async () => {
        const post = vi
            .spyOn(httpClient, 'post')
            .mockResolvedValue(
                successfulResponse(createdInvitation),
            )
        const patch = vi
            .spyOn(httpClient, 'patch')
            .mockResolvedValue(successfulResponse({
                ...invitation,
                status: 'REVOKED',
                active: false,
            }))
        const input = {
            fullName: 'Grace User',
            email: 'grace@example.com',
            role: 'TENANT_USER' as const,
        }

        await invitationsApi.createInvitation(
            'tenant-1',
            input,
        )
        await invitationsApi.revokeInvitation(
            'tenant-1',
            'invitation-1',
        )

        expect(post).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/user-invitations',
            input,
        )
        expect(patch).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/user-invitations/invitation-1/revoke',
        )
    })

    it('accepts invitations through the public API client', async () => {
        const response = {
            user: {
                id: 'user-2',
                tenantId: 'tenant-1',
                fullName: 'Grace User',
                email: 'grace@example.com',
                role: 'TENANT_USER' as const,
                status: 'ACTIVE' as const,
                createdAt: '2026-08-01T10:32:00Z',
                updatedAt: '2026-08-01T10:32:00Z',
            },
            message: 'Invitation accepted successfully.',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))
        const input = {
            invitationToken: 'one-time-token',
            newPassword: 'Strong@123',
            confirmPassword: 'Strong@123',
        }

        await expect(
            invitationsApi.acceptInvitation(input),
        ).resolves.toEqual(response)

        expect(post).toHaveBeenCalledWith(
            '/api/user-invitations/accept',
            input,
        )
    })
})
