import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { appTheme } from '../../../theme/appTheme'
import { projectMembersApi } from '../../projects/api/projectMembersApi'
import { approvalsApi } from '../api/approvalsApi'
import type { ApprovalRequest, ApprovalRequestSummary } from '../types/approvals'
import { ApprovalWorkflowsPanel } from './ApprovalWorkflowsPanel'

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

const pendingRequest: ApprovalRequestSummary = {
    id: 'request-1',
    definitionId: 'definition-1',
    workflowId: 'workflow-1',
    taskId: '12345678-1111-2222-3333-444444444444',
    status: 'PENDING',
    currentStageIndex: 0,
    currentStageName: 'Manager review',
    createdAt: '2026-09-16T10:00:00Z',
    completedAt: null,
}

const approvedRequest: ApprovalRequest = {
    id: pendingRequest.id,
    projectId: 'project-1',
    definitionId: pendingRequest.definitionId,
    definitionVersion: 2,
    workflowId: pendingRequest.workflowId,
    workflowVersion: 3,
    workflowExecutionId: 'execution-1',
    workflowNodeKey: 'approval_1',
    taskId: pendingRequest.taskId,
    actorUserId: 'requester-1',
    status: 'APPROVED',
    currentStageIndex: 0,
    stages: [],
    createdAt: pendingRequest.createdAt,
    completedAt: '2026-09-16T10:05:00Z',
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
        <ApprovalWorkflowsPanel tenantId="tenant-1" projectId="project-1" canManage />,
        { wrapper: Wrapper },
    )
}

describe('ApprovalWorkflowsPanel', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(approvalsApi, 'listDefinitions').mockResolvedValue(page([]))
        vi.spyOn(approvalsApi, 'inbox').mockResolvedValue(page([pendingRequest]))
        vi.spyOn(approvalsApi, 'history').mockResolvedValue(page([]))
        vi.spyOn(projectMembersApi, 'getMembers').mockResolvedValue(page([]))
    })

    it('records a reviewer approval from the project-scoped inbox', async () => {
        const decide = vi.spyOn(approvalsApi, 'decide').mockResolvedValue(approvedRequest)
        renderPanel()

        fireEvent.click(await screen.findByRole('tab', { name: /inbox/i }))
        expect(await screen.findByText(/manager review/i)).toBeVisible()

        fireEvent.change(screen.getByLabelText(/decision comment/i), {
            target: { value: 'Reviewed against release criteria' },
        })
        fireEvent.click(screen.getByRole('button', { name: /^approve$/i }))

        await waitFor(() => expect(decide).toHaveBeenCalledTimes(1))
        expect(decide).toHaveBeenCalledWith('tenant-1', 'project-1', 'request-1', {
            outcome: 'APPROVED',
            comment: 'Reviewed against release criteria',
        })
    })
})
