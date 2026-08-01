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

import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import { projectsApi } from '../features/projects/api/projectsApi'
import type { TenantProject } from '../features/projects/types/projects'
import { appTheme } from '../theme/appTheme'
import type { PageResponse } from '../types/api'
import { ProjectsPage } from './ProjectsPage'

const tenantProject: TenantProject = {
    id: 'project-1',
    tenantId: 'tenant-1',
    name: 'Research workspace',
    description: 'Coordinate the research programme.',
    status: 'PLANNING',
    createdByUserId: 'user-1',
    createdByUserName: 'Ada Admin',
    createdByUserEmail: 'ada@example.com',
    createdAt: '2026-07-15T10:30:00Z',
    updatedAt: '2026-07-16T10:30:00Z',
}

const archivedProject: TenantProject = {
    ...tenantProject,
    id: 'project-2',
    name: 'Archived workspace',
    description: null,
    status: 'ARCHIVED',
}

const projectsPage: PageResponse<TenantProject> = {
    content: [tenantProject, archivedProject],
    page: 0,
    size: 10,
    totalElements: 2,
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
            queries: {
                retry: false,
            },
        },
    })
}

function renderProjectsPage(
    contextValue: AuthContextValue = authContextValue,
) {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <AuthContext.Provider value={contextValue}>
                        {children}
                    </AuthContext.Provider>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<ProjectsPage />, {
        wrapper: Wrapper,
    })
}

describe('ProjectsPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('shows a loading state while projects are pending', () => {
        vi.spyOn(projectsApi, 'getProjects').mockReturnValue(
            new Promise(() => undefined),
        )

        renderProjectsPage()

        expect(
            screen.getByRole('status', {
                name: /loading projects/i,
            }),
        ).toBeInTheDocument()
    })

    it('renders projects returned by the API', async () => {
        vi.spyOn(projectsApi, 'getProjects').mockResolvedValue(
            projectsPage,
        )

        renderProjectsPage()

        expect(
            await screen.findByText('Research workspace'),
        ).toBeInTheDocument()
        expect(
            screen.getByText(
                'Coordinate the research programme.',
            ),
        ).toBeInTheDocument()
        expect(
            screen.getAllByText('Ada Admin'),
        ).toHaveLength(2)
        expect(
            screen.getByText('Archived'),
        ).toBeInTheDocument()
    })

    it('submits server-side search and sorting', async () => {
        const user = userEvent.setup()
        const getProjects = vi
            .spyOn(projectsApi, 'getProjects')
            .mockResolvedValue(projectsPage)

        renderProjectsPage()
        await screen.findByText('Research workspace')

        await user.type(
            screen.getByLabelText(/search projects/i),
            '  research  ',
        )
        await user.click(
            screen.getByRole('button', {
                name: /^search$/i,
            }),
        )
        await user.click(
            screen.getByRole('button', {
                name: /^project$/i,
            }),
        )

        expect(getProjects).toHaveBeenLastCalledWith(
            'tenant-1',
            expect.objectContaining({
                page: 0,
                search: 'research',
                sortBy: 'name',
                sortDir: 'asc',
            }),
        )
    })

    it('retries after the projects request fails', async () => {
        const user = userEvent.setup()

        vi.spyOn(projectsApi, 'getProjects')
            .mockRejectedValueOnce(
                new Error('Projects service unavailable.'),
            )
            .mockResolvedValueOnce(projectsPage)

        renderProjectsPage()

        expect(
            await screen.findByText(
                'Projects service unavailable.',
            ),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /retry/i,
            }),
        )

        expect(
            await screen.findByText('Research workspace'),
        ).toBeInTheDocument()
    })

    it('keeps lifecycle controls hidden from tenant users', async () => {
        vi.spyOn(projectsApi, 'getProjects').mockResolvedValue(
            projectsPage,
        )

        renderProjectsPage({
            ...authContextValue,
            session: {
                ...authContextValue.session!,
                userId: 'user-2',
                role: 'TENANT_USER',
            },
        })

        await screen.findByText('Research workspace')

        expect(
            screen.queryByRole('button', {
                name: /create project/i,
            }),
        ).not.toBeInTheDocument()
        expect(
            screen.queryByRole('button', {
                name: /manage research workspace/i,
            }),
        ).not.toBeInTheDocument()
    })

    it('creates a project with normalized input', async () => {
        const user = userEvent.setup()
        const createProject = vi
            .spyOn(projectsApi, 'createProject')
            .mockResolvedValue(tenantProject)
        vi.spyOn(projectsApi, 'getProjects').mockResolvedValue(
            projectsPage,
        )

        renderProjectsPage()
        await screen.findByText('Research workspace')
        await user.click(
            screen.getByRole('button', {
                name: /create project/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /create project/i,
        })

        await user.type(
            within(dialog).getByLabelText(/project name/i),
            '  Research workspace  ',
        )
        await user.type(
            within(dialog).getByLabelText(/description/i),
            '  Coordinate the research programme.  ',
        )
        await user.click(
            within(dialog).getByRole('button', {
                name: /create project/i,
            }),
        )

        await waitFor(() => {
            expect(createProject).toHaveBeenCalledWith(
                'tenant-1',
                {
                    name: 'Research workspace',
                    description:
                        'Coordinate the research programme.',
                },
            )
        })
    })

    it('edits a project as a tenant manager', async () => {
        const user = userEvent.setup()
        const updateProject = vi
            .spyOn(projectsApi, 'updateProject')
            .mockResolvedValue({
                ...tenantProject,
                name: 'Research platform',
            })
        vi.spyOn(projectsApi, 'getProjects').mockResolvedValue(
            projectsPage,
        )

        renderProjectsPage({
            ...authContextValue,
            session: {
                ...authContextValue.session!,
                userId: 'manager-1',
                role: 'TENANT_MANAGER',
            },
        })

        await screen.findByText('Research workspace')
        await user.click(
            screen.getByRole('button', {
                name: /manage research workspace/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /edit project/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /edit project/i,
        })
        const nameInput = within(dialog).getByLabelText(
            /project name/i,
        )

        await user.clear(nameInput)
        await user.type(nameInput, 'Research platform')
        await user.click(
            within(dialog).getByRole('button', {
                name: /save project/i,
            }),
        )

        await waitFor(() => {
            expect(updateProject).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                {
                    name: 'Research platform',
                    description:
                        'Coordinate the research programme.',
                },
            )
        })
    })

    it('changes a project status', async () => {
        const user = userEvent.setup()
        const updateStatus = vi
            .spyOn(projectsApi, 'updateProjectStatus')
            .mockResolvedValue({
                ...tenantProject,
                status: 'ACTIVE',
            })
        vi.spyOn(projectsApi, 'getProjects').mockResolvedValue(
            projectsPage,
        )

        renderProjectsPage()
        await screen.findByText('Research workspace')
        await user.click(
            screen.getByRole('button', {
                name: /manage research workspace/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /change status/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /change project status/i,
        })
        await user.click(
            within(dialog).getByLabelText(/^status$/i),
        )
        await user.click(
            screen.getByRole('option', {
                name: 'Active',
            }),
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
                { status: 'ACTIVE' },
            )
        })
    })

    it('archives a project through confirmation', async () => {
        const user = userEvent.setup()
        const archiveProject = vi
            .spyOn(projectsApi, 'archiveProject')
            .mockResolvedValue({
                ...tenantProject,
                status: 'ARCHIVED',
            })
        vi.spyOn(projectsApi, 'getProjects').mockResolvedValue(
            projectsPage,
        )

        renderProjectsPage()
        await screen.findByText('Research workspace')
        await user.click(
            screen.getByRole('button', {
                name: /manage research workspace/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /archive project/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /archive project/i,
        })
        await user.click(
            within(dialog).getByRole('button', {
                name: /archive project/i,
            }),
        )

        await waitFor(() => {
            expect(archiveProject).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
            )
        })
    })
})
