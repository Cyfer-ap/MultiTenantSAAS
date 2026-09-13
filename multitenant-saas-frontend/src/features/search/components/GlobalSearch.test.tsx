import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { globalSearchApi } from '../api/globalSearchApi'
import { GlobalSearch } from './GlobalSearch'

function LocationProbe() {
    const location = useLocation()
    return <output aria-label="Current location">{location.pathname + location.search}</output>
}

function renderSearch() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
        },
    })

    return render(
        <ThemeProvider theme={appTheme}>
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/dashboard']}>
                    <GlobalSearch tenantId="tenant-1" />
                    <LocationProbe />
                </MemoryRouter>
            </QueryClientProvider>
        </ThemeProvider>,
    )
}

describe('GlobalSearch', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('searches accessible results and deep-links to a task', async () => {
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

        renderSearch()
        await user.click(screen.getByRole('button', { name: /search workspace/i }))
        await user.type(
            screen.getByPlaceholderText(/search projects, tasks, and people/i),
            'phoenix',
        )

        const result = await screen.findByRole('button', { name: /phoenix launch checklist/i })
        expect(search).toHaveBeenCalledWith('tenant-1', 'phoenix', 12)

        await user.click(result)
        expect(screen.getByLabelText('Current location')).toHaveTextContent(
            '/projects/project-1?task=task-1',
        )
    })

    it('shows the empty state when no accessible result matches', async () => {
        const user = userEvent.setup()
        vi.spyOn(globalSearchApi, 'search').mockResolvedValue({ query: 'missing', results: [] })

        renderSearch()
        await user.click(screen.getByRole('button', { name: /search workspace/i }))
        await user.type(
            screen.getByPlaceholderText(/search projects, tasks, and people/i),
            'missing',
        )

        expect(await screen.findByText('No results')).toBeInTheDocument()
        expect(screen.getByText(/no accessible projects, tasks, or people/i)).toBeInTheDocument()
    })

    it('shows an error without exposing server details', async () => {
        const user = userEvent.setup()
        vi.spyOn(globalSearchApi, 'search').mockRejectedValue(new Error('database detail'))

        renderSearch()
        await user.click(screen.getByRole('button', { name: /search workspace/i }))
        await user.type(
            screen.getByPlaceholderText(/search projects, tasks, and people/i),
            'broken',
        )

        await waitFor(() => {
            expect(screen.getByText(/search could not be completed/i)).toBeInTheDocument()
        })
        expect(screen.queryByText(/database detail/i)).not.toBeInTheDocument()
    })
})
