import FolderRoundedIcon from '@mui/icons-material/FolderRounded'
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded'
import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { globalSearchApi } from '../../search/api/globalSearchApi'
import { CommandPalette } from './CommandPalette'

function LocationProbe() {
    const location = useLocation()
    return <output aria-label="Current location">{location.pathname + location.search}</output>
}

const navigationItems = [
    {
        label: 'Projects',
        path: '/projects',
        icon: <FolderRoundedIcon />,
    },
    {
        label: 'Users',
        path: '/users',
        icon: <GroupsRoundedIcon />,
    },
] as const

function renderPalette({
    canCreateProject = true,
    canInviteUser = true,
}: {
    canCreateProject?: boolean
    canInviteUser?: boolean
} = {}) {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })

    return render(
        <ThemeProvider theme={appTheme}>
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/dashboard']}>
                    <CommandPalette
                        canCreateProject={canCreateProject}
                        canInviteUser={canInviteUser}
                        navigationItems={navigationItems}
                        tenantId="tenant-1"
                    />
                    <LocationProbe />
                </MemoryRouter>
            </QueryClientProvider>
        </ThemeProvider>,
    )
}

describe('CommandPalette', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('opens with Ctrl+K and only exposes allowed quick actions', async () => {
        renderPalette({ canCreateProject: true, canInviteUser: false })

        fireEvent.keyDown(document, { key: 'k', ctrlKey: true })

        expect(await screen.findByRole('dialog', { name: /command palette/i })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /create project/i })).toBeInTheDocument()
        expect(screen.queryByRole('button', { name: /invite user/i })).not.toBeInTheDocument()
        expect(screen.getByRole('button', { name: /open projects/i })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /open users/i })).toBeInTheDocument()
    })

    it('opens the existing create-project flow from the palette', async () => {
        const user = userEvent.setup()
        renderPalette()

        await user.click(screen.getByRole('button', { name: /open command palette/i }))
        await user.click(screen.getByRole('button', { name: /create project/i }))

        expect(screen.getByRole('heading', { name: /create project/i })).toBeInTheDocument()
        expect(screen.queryByRole('dialog', { name: /command palette/i })).not.toBeInTheDocument()
    })

    it('searches accessible entities and deep-links to a task', async () => {
        const user = userEvent.setup()
        const search = vi.spyOn(globalSearchApi, 'search').mockResolvedValue({
            query: 'phoenix',
            results: [
                {
                    type: 'TASK',
                    id: 'task-1',
                    parentId: 'project-1',
                    title: 'Phoenix launch checklist',
                    subtitle: 'Phoenix',
                },
            ],
        })

        renderPalette()
        await user.click(screen.getByRole('button', { name: /open command palette/i }))
        await user.type(screen.getByPlaceholderText(/search or type a command/i), 'phoenix')

        const result = await screen.findByRole('button', { name: /phoenix launch checklist/i })
        expect(search).toHaveBeenCalledWith('tenant-1', 'phoenix', 12)

        await user.click(result)
        expect(screen.getByLabelText('Current location')).toHaveTextContent(
            '/projects/project-1?task=task-1',
        )
    })

    it('supports keyboard execution for filtered navigation commands', async () => {
        const user = userEvent.setup()
        renderPalette({ canCreateProject: false, canInviteUser: false })

        fireEvent.keyDown(document, { key: 'k', ctrlKey: true })
        const input = await screen.findByPlaceholderText(/search or type a command/i)
        await user.type(input, 'open projects')
        await user.keyboard('{Enter}')

        expect(screen.getByLabelText('Current location')).toHaveTextContent('/projects')
    })
})
