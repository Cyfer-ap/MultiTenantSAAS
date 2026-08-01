import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
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
import {
    MemoryRouter,
    Route,
    Routes,
} from 'react-router'

import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import { projectMembersApi } from '../features/projects/api/projectMembersApi'
import { projectsApi } from '../features/projects/api/projectsApi'
import type {
    ProjectMember,
    TenantProject,
} from '../features/projects/types/projects'
import { appTheme } from '../theme/appTheme'
import type { PageResponse } from '../types/api'
import { ProjectDetailsPage } from './ProjectDetailsPage'

const tenantProject: TenantProject = {
    id: 'project-1',
    tenantId: 'tenant-1',
    name: 'Research workspace',
    description: 'Coordinate the research programme.',
    status: 'ACTIVE',
    createdByUserId: 'user-1',
    createdByUserName: 'Ada Admin',
    createdByUserEmail: 'ada@example.com',
    createdAt: '2026-07-15T10:30:00Z',
    updatedAt: '2026-07-16T10:30:00Z',
}

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

const membersPage: PageResponse<ProjectMember> = {
    content: [projectMember],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
}

const authContextValue: AuthContextValue = {
    status: 'authenticated',
    session: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        tokenType: 'Bearer',
        accessTokenExpiresAt: Date.now() + 60_000,
        tenantId: 'tenant-1',
        userId: 'user-1',
        fullName: 'Ada Admin',
        email: 'ada@example.com',
        role: 'TENANT_ADMIN',
    },
    login: vi.fn(),
    logout: vi.fn(),
}

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })
}

function renderProjectDetailsPage(
    contextValue: AuthContextValue = authContextValue,
) {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter
                        initialEntries={['/projects/project-1']}
                    >
                        <AuthContext.Provider value={contextValue}>
                            <Routes>
                                <Route
                                    path="/projects/:projectId"
                                    element={children}
                                />
                            </Routes>
                        </AuthContext.Provider>
                    </MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<ProjectDetailsPage />, {
        wrapper: Wrapper,
    })
}

describe('ProjectDetailsPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(projectsApi, 'getProject').mockResolvedValue(
            tenantProject,
        )
        vi.spyOn(
            projectMembersApi,
            'getMembers',
        ).mockResolvedValue(membersPage)
    })

    it('renders project metadata and its member directory', async () => {
        renderProjectDetailsPage()

        expect(
            await screen.findByRole('heading', {
                name: 'Research workspace',
            }),
        ).toBeInTheDocument()
        expect(
            screen.getByText(
                'Coordinate the research programme.',
            ),
        ).toBeInTheDocument()
        expect(
            await screen.findByText('Grace User'),
        ).toBeInTheDocument()
        expect(
            screen.getByText('grace@example.com'),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('button', {
                name: /add member/i,
            }),
        ).toBeInTheDocument()
    })

    it('submits server-side member search and role filters', async () => {
        const user = userEvent.setup()
        const getMembers = vi
            .spyOn(projectMembersApi, 'getMembers')
            .mockResolvedValue(membersPage)

        renderProjectDetailsPage()
        await screen.findByText('Grace User')

        await user.type(
            screen.getByLabelText(/search project members/i),
            '  grace  ',
        )
        await user.click(
            screen.getByLabelText(/^project role$/i),
        )
        await user.click(
            screen.getByRole('option', { name: 'Member' }),
        )
        await user.click(
            screen.getByRole('button', { name: /^search$/i }),
        )

        expect(getMembers).toHaveBeenLastCalledWith(
            'tenant-1',
            'project-1',
            expect.objectContaining({
                page: 0,
                role: 'MEMBER',
                search: 'grace',
            }),
        )
    })

    it('changes a member project role', async () => {
        const user = userEvent.setup()
        const updateMemberRole = vi
            .spyOn(projectMembersApi, 'updateMemberRole')
            .mockResolvedValue({
                ...projectMember,
                projectRole: 'PROJECT_LEAD',
            })

        renderProjectDetailsPage()
        await screen.findByText('Grace User')
        await user.click(
            screen.getByRole('button', {
                name: /manage grace user/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /change project role/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /change project role/i,
        })
        await user.click(
            within(dialog).getByLabelText(/project role/i),
        )
        await user.click(
            screen.getByRole('option', {
                name: /project lead/i,
            }),
        )
        await user.click(
            within(dialog).getByRole('button', {
                name: /change role/i,
            }),
        )

        await waitFor(() => {
            expect(updateMemberRole).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                'user-2',
                { role: 'PROJECT_LEAD' },
            )
        })
    })

    it('keeps membership controls hidden from tenant users', async () => {
        renderProjectDetailsPage({
            ...authContextValue,
            session: {
                ...authContextValue.session!,
                userId: 'user-2',
                role: 'TENANT_USER',
            },
        })

        await screen.findByText('Grace User')

        expect(
            screen.queryByRole('button', {
                name: /add member/i,
            }),
        ).not.toBeInTheDocument()
        expect(
            screen.queryByRole('button', {
                name: /manage grace user/i,
            }),
        ).not.toBeInTheDocument()
    })

    it('makes archived project membership read-only', async () => {
        vi.spyOn(projectsApi, 'getProject').mockResolvedValue({
            ...tenantProject,
            status: 'ARCHIVED',
        })

        renderProjectDetailsPage()
        await screen.findByText('Grace User')

        expect(
            screen.getByText(/memberships remain visible/i),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('button', {
                name: /add member/i,
            }),
        ).toBeDisabled()
        expect(
            screen.getByRole('button', {
                name: /manage grace user/i,
            }),
        ).toBeDisabled()
    })
})
