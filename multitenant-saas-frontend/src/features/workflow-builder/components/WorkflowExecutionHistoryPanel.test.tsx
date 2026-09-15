import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { workflowsApi } from '../api/workflowsApi'
import type { WorkflowDefinition, WorkflowExecution } from '../types/workflows'
import { WorkflowExecutionHistoryPanel } from './WorkflowExecutionHistoryPanel'

const workflow: WorkflowDefinition = {
    id: 'workflow-1',
    tenantId: 'tenant-1',
    createdByUserId: 'user-1',
    name: 'Urgent triage',
    description: null,
    status: 'ACTIVE',
    definitionVersion: 3,
    nodes: [],
    edges: [],
    createdAt: '2026-09-14T10:00:00Z',
    updatedAt: '2026-09-14T10:05:00Z',
}

const execution: WorkflowExecution = {
    id: 'execution-1',
    workflowId: 'workflow-1',
    workflowVersion: 3,
    triggerOperation: 'TRIGGER_TASK_CREATED',
    sourceEntityType: 'TASK',
    sourceEntityId: 'task-12345678',
    status: 'SUCCEEDED',
    explanation: 'Applied ACTION_SET_TASK_STATUS',
    errorMessage: null,
    startedAt: '2026-09-14T11:00:00Z',
    completedAt: '2026-09-14T11:00:01Z',
}

function page<T>(content: T[]) {
    return {
        content,
        page: 0,
        size: 25,
        totalElements: content.length,
        totalPages: content.length ? 1 : 0,
        first: true,
        last: true,
    }
}

function renderPanel() {
    const queryClient = new QueryClient({
        defaultOptions: { queries: { retry: false } },
    })

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<WorkflowExecutionHistoryPanel tenantId="tenant-1" canRead />, {
        wrapper: Wrapper,
    })
}

describe('WorkflowExecutionHistoryPanel', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('renders tenant workflow executions with workflow names and outcomes', async () => {
        vi.spyOn(workflowsApi, 'list').mockResolvedValue(page([workflow]))
        vi.spyOn(workflowsApi, 'executions').mockResolvedValue(page([execution]))

        renderPanel()

        expect(await screen.findByText('Urgent triage')).toBeInTheDocument()
        expect(screen.getByText('SUCCEEDED')).toBeInTheDocument()
        expect(screen.getByText('Applied ACTION_SET_TASK_STATUS')).toBeInTheDocument()
        expect(screen.getByText(/task-123/i)).toBeInTheDocument()
    })
})
