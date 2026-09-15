import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '../../auth/context/AuthContext'
import type { AuthContextValue } from '../../auth/context/AuthContext'
import { currentAuthorizationQueryKeys } from '../../authorization/hooks/useCurrentAuthorization'
import { createProjectAuthorizationContext } from '../../authorization/test/authorizationTestData'
import { authorizationPermissionCodes } from '../../authorization/types/authorization'
import { projectMembersApi } from '../../projects/api/projectMembersApi'
import { projectsApi } from '../../projects/api/projectsApi'
import type { ProjectMember, TenantProject } from '../../projects/types/projects'
import { appTheme } from '../../../theme/appTheme'
import type { PageResponse } from '../../../types/api'
import { whiteboardsApi } from '../api/whiteboardsApi'
import type { Whiteboard, WhiteboardSummary } from '../types/whiteboards'
import { WhiteboardWorkspacePage } from './WhiteboardWorkspacePage'

const project: TenantProject = {
    id: 'project-1',
    tenantId: 'tenant-1',
    name: 'Launch project',
    description: 'Plan the launch.',
    status: 'ACTIVE',
    createdByUserId: 'user-1',
    createdByUserName: 'Ada Admin',
    createdByUserEmail: 'ada@example.com',
    createdAt: '2026-09-01T10:00:00Z',
    updatedAt: '2026-09-10T10:00:00Z',
}

const projectLead: ProjectMember = {
    membershipId: 'membership-1',
    projectId: 'project-1',
    userId: 'user-1',
    fullName: 'Ada Admin',
    email: 'ada@example.com',
    tenantRole: 'TENANT_USER',
    userStatus: 'ACTIVE',
    projectRole: 'PROJECT_LEAD',
    assignedByUserId: 'user-1',
    assignedByUserName: 'Ada Admin',
    assignedByUserEmail: 'ada@example.com',
    assignedAt: '2026-09-01T10:00:00Z',
    updatedAt: '2026-09-01T10:00:00Z',
}

const memberPage: PageResponse<ProjectMember> = {
    content: [projectLead],
    page: 0,
    size: 100,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

const emptyBoards: PageResponse<WhiteboardSummary> = {
    content: [],
    page: 0,
    size: 100,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
}

const board: Whiteboard = {
    id: 'board-1',
    tenantId: 'tenant-1',
    projectId: 'project-1',
    createdByUserId: 'user-1',
    name: 'Launch map',
    version: 0,
    nodes: [],
    edges: [],
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
}

const authContextValue: AuthContextValue = {
    status: 'authenticated',
    session: {
        accessToken: 'access-token',
        csrfToken: 'csrf-token',
        persistentSession: true,
        tokenType: 'Bearer',
        accessTokenExpiresAt: Date.now() + 60_000,
        tenantId: 'tenant-1',
        userId: 'user-1',
        fullName: 'Ada Admin',
        email: 'ada@example.com',
        role: 'TENANT_USER',
    },
    login: vi.fn(),
    logout: vi.fn(),
}

function renderWorkspace(permissionCodes: string[]) {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })
    queryClient.setQueryData(
        currentAuthorizationQueryKeys.current('tenant-1', 'user-1'),
        createProjectAuthorizationContext({
            userId: 'user-1',
            projectId: 'project-1',
            permissionCodes,
        }),
    )

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/projects/project-1/whiteboards']}>
                        <AuthContext.Provider value={authContextValue}>
                            <Routes>
                                <Route path="/projects/:projectId/whiteboards" element={children} />
                            </Routes>
                        </AuthContext.Provider>
                    </MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<WhiteboardWorkspacePage />, { wrapper: Wrapper })
}

describe('WhiteboardWorkspacePage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(projectsApi, 'getProject').mockResolvedValue(project)
        vi.spyOn(projectMembersApi, 'getMembers').mockResolvedValue(memberPage)
        vi.spyOn(projectMembersApi, 'getMember').mockResolvedValue(projectLead)
        vi.spyOn(whiteboardsApi, 'list').mockResolvedValue(emptyBoards)
        vi.spyOn(whiteboardsApi, 'get').mockResolvedValue(board)
    })

    it('creates a board for a project task manager', async () => {
        const user = userEvent.setup()
        const create = vi.spyOn(whiteboardsApi, 'create').mockResolvedValue(board)

        renderWorkspace([
            authorizationPermissionCodes.PROJECT_READ,
            authorizationPermissionCodes.PROJECT_TASK_MANAGE,
        ])

        expect(
            await screen.findByRole('heading', { name: 'Collaborative Whiteboard' }),
        ).toBeInTheDocument()
        await user.type(screen.getByLabelText('New board name'), 'Launch map')
        await user.click(screen.getByRole('button', { name: 'Create board' }))

        await waitFor(() => {
            expect(create).toHaveBeenCalledWith('tenant-1', 'project-1', {
                name: 'Launch map',
                nodes: [],
                edges: [],
            })
        })
        expect(await screen.findByText('Whiteboard created')).toBeInTheDocument()
    })

    it('lets a project lead edit even without an explicit manage grant', async () => {
        renderWorkspace([authorizationPermissionCodes.PROJECT_READ])

        expect(await screen.findByLabelText('New board name')).toBeInTheDocument()
        expect(projectMembersApi.getMember).toHaveBeenCalledWith('tenant-1', 'project-1', 'user-1')
    })

    it('keeps archived project boards read-only', async () => {
        vi.spyOn(projectsApi, 'getProject').mockResolvedValue({ ...project, status: 'ARCHIVED' })
        vi.spyOn(whiteboardsApi, 'list').mockResolvedValue({
            ...emptyBoards,
            content: [
                {
                    id: board.id,
                    tenantId: board.tenantId,
                    projectId: board.projectId,
                    createdByUserId: board.createdByUserId,
                    name: board.name,
                    version: board.version,
                    createdAt: board.createdAt,
                    updatedAt: board.updatedAt,
                },
            ],
            totalElements: 1,
            totalPages: 1,
        })

        renderWorkspace([
            authorizationPermissionCodes.PROJECT_READ,
            authorizationPermissionCodes.PROJECT_TASK_MANAGE,
        ])

        expect(
            await screen.findByText(/whiteboards remain visible but cannot be changed/i),
        ).toBeInTheDocument()
        expect(screen.queryByLabelText('New board name')).not.toBeInTheDocument()
        expect(await screen.findByText(/read-only access to this whiteboard/i)).toBeInTheDocument()
    })
})
