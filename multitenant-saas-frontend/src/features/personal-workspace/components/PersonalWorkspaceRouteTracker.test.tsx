import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { PersonalWorkspaceRouteTracker } from './PersonalWorkspaceRouteTracker'

const mocks = vi.hoisted(() => ({
    favorite: vi.fn(),
    unfavorite: vi.fn(),
    recordRecent: vi.fn(),
    favorites: [] as Array<{ type: 'PROJECT' | 'TASK'; resourceId: string }>,
}))

vi.mock('../hooks/usePersonalWorkspace', () => ({
    usePersonalWorkspace: () => ({
        data: {
            favorites: mocks.favorites,
            recent: [],
        },
        isPending: false,
    }),
    useFavoritePersonalResource: () => ({
        mutate: mocks.favorite,
        isPending: false,
    }),
    useUnfavoritePersonalResource: () => ({
        mutate: mocks.unfavorite,
        isPending: false,
    }),
    useRecordRecentPersonalResource: () => ({
        mutate: mocks.recordRecent,
    }),
}))

function renderTracker(entry: string) {
    return render(
        <MemoryRouter initialEntries={[entry]}>
            <PersonalWorkspaceRouteTracker tenantId="tenant-1" />
        </MemoryRouter>,
    )
}

describe('PersonalWorkspaceRouteTracker', () => {
    beforeEach(() => {
        mocks.favorite.mockReset()
        mocks.unfavorite.mockReset()
        mocks.recordRecent.mockReset()
        mocks.favorites.length = 0
    })

    it('lets the current project be favorited directly from its route', async () => {
        const user = userEvent.setup()
        renderTracker('/projects/project-1')

        await waitFor(() => {
            expect(mocks.recordRecent).toHaveBeenCalledWith({
                type: 'PROJECT',
                resourceId: 'project-1',
            })
        })

        await user.click(screen.getByRole('button', { name: 'Add project to favorites' }))

        expect(mocks.favorite).toHaveBeenCalledWith({
            type: 'PROJECT',
            resourceId: 'project-1',
        })
    })

    it('switches the contextual control to an open task and can unfavorite it', async () => {
        const user = userEvent.setup()
        mocks.favorites.push({ type: 'TASK', resourceId: 'task-1' })
        renderTracker('/projects/project-1?task=task-1')

        await waitFor(() => {
            expect(mocks.recordRecent).toHaveBeenCalledWith({
                type: 'TASK',
                resourceId: 'task-1',
            })
        })

        await user.click(screen.getByRole('button', { name: 'Remove task from favorites' }))

        expect(mocks.unfavorite).toHaveBeenCalledWith({
            type: 'TASK',
            resourceId: 'task-1',
        })
    })

    it('keeps the contextual favorite control off unrelated workspace routes', () => {
        renderTracker('/users')

        expect(screen.queryByRole('button', { name: /favorites/i })).not.toBeInTheDocument()
        expect(mocks.recordRecent).not.toHaveBeenCalled()
    })
})
