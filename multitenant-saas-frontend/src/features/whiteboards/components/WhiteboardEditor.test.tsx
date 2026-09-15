import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { whiteboardsApi } from '../api/whiteboardsApi'
import type { Whiteboard } from '../types/whiteboards'
import { WhiteboardEditor } from './WhiteboardEditor'

const board: Whiteboard = {
    id: 'board-1',
    tenantId: 'tenant-1',
    projectId: 'project-1',
    createdByUserId: 'user-1',
    name: 'Launch map',
    version: 2,
    nodes: [
        {
            id: 'node-id-1',
            key: 'sticky_1',
            type: 'STICKY',
            content: 'Prepare launch checklist',
            linkedTaskId: null,
            x: 100,
            y: 120,
            width: 220,
            height: 150,
            zIndex: 1,
        },
    ],
    edges: [],
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
}

function renderEditor() {
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
        <WhiteboardEditor
            board={board}
            canManage
            members={[]}
            onReload={vi.fn().mockResolvedValue(undefined)}
            projectId="project-1"
            tenantId="tenant-1"
        />,
        { wrapper: Wrapper },
    )
}

describe('WhiteboardEditor', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('autosaves a newly added sticky against the current board version', async () => {
        const user = userEvent.setup()
        const update = vi.spyOn(whiteboardsApi, 'update').mockImplementation(
            async (_tenantId, _projectId, _boardId, input) => ({
                ...board,
                version: 3,
                name: input.name,
                nodes: input.nodes.map((node, index) => ({
                    ...node,
                    id: `node-${index}`,
                    linkedTaskId: null,
                })),
                edges: input.edges.map((edge, index) => ({ ...edge, id: `edge-${index}` })),
            }),
        )

        renderEditor()
        await user.click(screen.getByRole('button', { name: 'Sticky' }))

        await waitFor(() => {
            expect(update).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                'board-1',
                expect.objectContaining({
                    expectedVersion: 2,
                    name: 'Launch map',
                    nodes: expect.arrayContaining([
                        expect.objectContaining({ type: 'STICKY', content: 'New sticky note' }),
                    ]),
                }),
            )
        })
        expect(await screen.findByText('Sticky added')).toBeInTheDocument()
        expect(screen.getByText('v3')).toBeInTheDocument()
    })

    it('converts a sticky node to an ordinary project task', async () => {
        const user = userEvent.setup()
        vi.spyOn(whiteboardsApi, 'convertNodeToTask').mockResolvedValue({
            boardId: 'board-1',
            nodeKey: 'sticky_1',
            taskId: 'task-1',
            boardVersion: 3,
            createdAt: '2026-09-15T11:00:00Z',
        })

        renderEditor()
        fireEvent.pointerDown(screen.getByLabelText('Sticky node'), {
            pointerId: 1,
            clientX: 100,
            clientY: 120,
        })
        await user.click(screen.getByRole('button', { name: 'Convert to task' }))

        const dialog = screen.getByRole('dialog', { name: 'Convert node to task' })
        const title = within(dialog).getByLabelText('Task title')
        await user.clear(title)
        await user.type(title, 'Ship launch checklist')
        await user.click(within(dialog).getByRole('button', { name: 'Create task' }))

        await waitFor(() => {
            expect(whiteboardsApi.convertNodeToTask).toHaveBeenCalledWith(
                'tenant-1',
                'project-1',
                'board-1',
                'sticky_1',
                expect.objectContaining({
                    expectedVersion: 2,
                    title: 'Ship launch checklist',
                    priority: 'MEDIUM',
                    assigneeUserId: null,
                }),
            )
        })
        expect(await screen.findByText('Task created and linked to this node')).toBeInTheDocument()
        expect(screen.getAllByText('Task linked').length).toBeGreaterThan(0)
        expect(screen.getByText('v3')).toBeInTheDocument()
    })
})
