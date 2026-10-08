import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { projectTemplatesApi } from '../api/projectTemplatesApi'
import type { ProjectTemplate } from '../types/projectTemplates'
import { ProjectTemplatesPanel } from './ProjectTemplatesPanel'

function page(content: ProjectTemplate[]) {
    return {
        content,
        page: 0,
        size: 50,
        totalElements: content.length,
        totalPages: content.length ? 1 : 0,
        first: true,
        last: true,
    }
}

function renderPanel() {
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
        <ProjectTemplatesPanel tenantId="tenant-1" canRead canManage />,
        { wrapper: Wrapper },
    )
}

const savedTemplate: ProjectTemplate = {
    id: 'template-1',
    tenantId: 'tenant-1',
    createdByUserId: 'user-1',
    name: 'Temp',
    projectNameSeed: 'Project1',
    projectDescription: 'test',
    initialStatus: 'PLANNING',
    tasks: [],
    createdAt: '2026-10-08T10:00:00Z',
    updatedAt: '2026-10-08T10:00:00Z',
}

describe('ProjectTemplatesPanel', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(projectTemplatesApi, 'list').mockResolvedValue(page([]))
    })

    it('creates a project template with zero starter tasks', async () => {
        const create = vi.spyOn(projectTemplatesApi, 'create').mockResolvedValue(savedTemplate)

        renderPanel()

        fireEvent.change(screen.getByLabelText(/template name/i), {
            target: { value: 'Temp' },
        })
        fireEvent.change(screen.getByLabelText(/default project name/i), {
            target: { value: 'Project1' },
        })
        fireEvent.change(screen.getByLabelText(/project description/i), {
            target: { value: 'test' },
        })
        fireEvent.click(screen.getByRole('button', { name: /create template/i }))

        await waitFor(() => expect(create).toHaveBeenCalledTimes(1))
        expect(create).toHaveBeenCalledWith('tenant-1', {
            name: 'Temp',
            projectNameSeed: 'Project1',
            projectDescription: 'test',
            initialStatus: 'PLANNING',
            tasks: [],
        })
    })

    it('shows the normalized backend reason when saving fails', async () => {
        vi.spyOn(projectTemplatesApi, 'create').mockRejectedValue(
            new Error('This workspace is read-only because its subscription has expired.'),
        )

        renderPanel()

        fireEvent.change(screen.getByLabelText(/template name/i), {
            target: { value: 'Temp' },
        })
        fireEvent.change(screen.getByLabelText(/default project name/i), {
            target: { value: 'Project1' },
        })
        fireEvent.click(screen.getByRole('button', { name: /create template/i }))

        expect(
            await screen.findByText(
                'This workspace is read-only because its subscription has expired.',
            ),
        ).toBeVisible()
    })
})
