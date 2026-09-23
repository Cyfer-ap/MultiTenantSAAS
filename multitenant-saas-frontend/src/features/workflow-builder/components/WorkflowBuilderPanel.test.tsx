import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { approvalsApi } from '../../approvals/api/approvalsApi'
import { workflowsApi } from '../api/workflowsApi'
import type { WorkflowDefinition } from '../types/workflows'
import { WorkflowBuilderPanel } from './WorkflowBuilderPanel'

const activeWorkflow: WorkflowDefinition = {
    id: 'workflow-1',
    tenantId: 'tenant-1',
    createdByUserId: 'user-1',
    name: 'Active workflow',
    description: 'Routes urgent tasks',
    status: 'ACTIVE',
    definitionVersion: 2,
    nodes: [
        {
            id: 'node-1',
            key: 'trigger',
            type: 'TRIGGER',
            operation: 'TRIGGER_TASK_CREATED',
            configuration: {},
            x: 60,
            y: 150,
        },
        {
            id: 'node-2',
            key: 'action',
            type: 'ACTION',
            operation: 'ACTION_SET_TASK_PRIORITY',
            configuration: { value: 'HIGH' },
            x: 420,
            y: 150,
        },
    ],
    edges: [
        {
            id: 'edge-1',
            sourceKey: 'trigger',
            targetKey: 'action',
            branch: 'DEFAULT',
        },
    ],
    createdAt: '2026-09-14T10:00:00Z',
    updatedAt: '2026-09-14T10:05:00Z',
}

function page<T>(content: T[]) {
    return {
        content,
        page: 0,
        size: 100,
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
        <WorkflowBuilderPanel tenantId="tenant-1" projectId="project-1" canRead canManage />,
        { wrapper: Wrapper },
    )
}

describe('WorkflowBuilderPanel', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(approvalsApi, 'listDefinitions').mockResolvedValue(page([]))
    })

    it('creates a workflow from the valid starter graph', async () => {
        vi.spyOn(workflowsApi, 'list').mockResolvedValue(page([]))
        const create = vi.spyOn(workflowsApi, 'create').mockResolvedValue({
            ...activeWorkflow,
            id: 'workflow-created',
            name: 'Urgent triage',
            status: 'DRAFT',
            definitionVersion: 1,
        })

        renderPanel()

        fireEvent.change(screen.getByLabelText(/workflow name/i), {
            target: { value: 'Urgent triage' },
        })
        fireEvent.click(screen.getByRole('button', { name: /save draft/i }))

        await waitFor(() => expect(create).toHaveBeenCalledTimes(1))
        expect(create).toHaveBeenCalledWith(
            'tenant-1',
            expect.objectContaining({
                name: 'Urgent triage',
                nodes: expect.arrayContaining([
                    expect.objectContaining({
                        key: 'trigger',
                        operation: 'TRIGGER_TASK_CREATED',
                    }),
                    expect.objectContaining({
                        key: 'condition',
                        operation: 'CONDITION_TASK_PRIORITY_EQUALS',
                    }),
                    expect.objectContaining({
                        key: 'action',
                        operation: 'ACTION_SET_TASK_STATUS',
                    }),
                ]),
                edges: [
                    { sourceKey: 'trigger', targetKey: 'condition', branch: 'DEFAULT' },
                    { sourceKey: 'condition', targetKey: 'action', branch: 'TRUE' },
                ],
            }),
        )
        expect(approvalsApi.listDefinitions).toHaveBeenCalledWith('tenant-1', 'project-1')
    })

    it('keeps active workflows read-only until they are paused', async () => {
        vi.spyOn(workflowsApi, 'list').mockResolvedValue(page([activeWorkflow]))

        renderPanel()

        fireEvent.click(await screen.findByRole('button', { name: /active workflow/i }))

        expect(
            screen.getByText(/active workflows are read-only.*pause this workflow/i),
        ).toBeInTheDocument()
        expect(screen.queryByRole('button', { name: /save draft/i })).not.toBeInTheDocument()
        expect(screen.getByRole('button', { name: /^pause$/i })).toBeInTheDocument()
        expect(screen.getByLabelText(/workflow name/i)).toBeDisabled()
    })
})
